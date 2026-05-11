package com.nursenasevilmis.fenlab.service
import com.nursenasevilmis.fenlab.config.MinioProperties
import io.minio.*
import io.minio.http.Method
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class MinioService(
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties
) {

    @PostConstruct
    private fun createBucketsIfNotExists() {
        val buckets = listOf(
            minioProperties.buckets.videos,
            minioProperties.buckets.images,
            minioProperties.buckets.pdfs,
            minioProperties.buckets.profiles
        )

        buckets.forEach { bucketName ->
            val found = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            )

            if (!found) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                )
            }

            // ÖNEMLİ: Bucket önceden var olsa bile public read policy tekrar set edilsin
            setBucketPolicy(bucketName)
        }
    }

    private fun setBucketPolicy(bucketName: String) {
        val policy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {"AWS": "*"},
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::$bucketName/*"]
                }
              ]
            }
        """.trimIndent()

        minioClient.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(policy)
                .build()
        )
    }

    /**
     * Video yükleme — DB'ye "fenlab-videos/dosya.mp4" şeklinde path döner
     */
    fun uploadVideo(file: MultipartFile): String {
        val fileName = generateFileName(file.originalFilename ?: "video", "mp4")
        return uploadFile(
            file.inputStream,
            fileName,
            file.contentType ?: "video/mp4",
            minioProperties.buckets.videos
        )
    }

    /**
     * Resim yükleme — DB'ye "fenlab-images/dosya.jpg" şeklinde path döner
     */
    fun uploadImage(file: MultipartFile): String {
        val fileName = generateFileName(file.originalFilename ?: "image", "jpg")
        return uploadFile(
            file.inputStream,
            fileName,
            file.contentType ?: "image/jpeg",
            minioProperties.buckets.images
        )
    }

    /**
     * Profil resmi yükleme — DB'ye "fenlab-profiles/dosya.jpg" şeklinde path döner
     */
    fun uploadProfileImage(file: MultipartFile): String {
        val fileName = generateFileName(file.originalFilename ?: "profile", "jpg")
        return uploadFile(
            file.inputStream,
            fileName,
            file.contentType ?: "image/jpeg",
            minioProperties.buckets.profiles
        )
    }

    /**
     * PDF yükleme — DB'ye "fenlab-pdfs/dosya.pdf" şeklinde path döner
     */
    fun uploadPdf(inputStream: InputStream, fileName: String): String {
        val pdfFileName = generateFileName(fileName, "pdf")
        return uploadFile(
            inputStream,
            pdfFileName,
            "application/pdf",
            minioProperties.buckets.pdfs
        )
    }

    /**
     * Genel dosya yükleme — artık tam URL değil, sadece path döner
     */
    private fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        contentType: String,
        bucketName: String
    ): String {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .stream(inputStream, inputStream.available().toLong(), -1)
                    .contentType(contentType)
                    .build()
            )
            // Tam URL değil, sadece path kaydediliyor → IP değişince DB bozulmaz
            return getObjectPath(bucketName, fileName)
        } catch (e: Exception) {
            throw RuntimeException("Dosya yüklenirken hata oluştu: ${e.message}", e)
        }
    }

    /**
     * DB'ye kaydedilen path'den tam URL üretir.
     * IP sadece application.properties'deki minio.public-url'den okunur.
     */
    fun pathToUrl(objectPath: String): String {
        val baseUrl = minioProperties.publicUrl.ifBlank { minioProperties.url }
        return "$baseUrl/$objectPath"
    }

    /**
     * Sadece path döner: "fenlab-profiles/foto.jpg"
     * Bunu doğrudan DB'ye kaydet.
     */
    fun getObjectPath(bucketName: String, fileName: String): String {
        return "$bucketName/$fileName"
    }

    /**
     * Geçici URL oluşturma (presigned URL)
     */
    fun getPresignedUrl(bucketName: String, fileName: String, expiryMinutes: Int = 60): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(fileName)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build()
        )
    }

    /**
     * Dosya silme — path veya tam URL ile çalışır
     */
    fun deleteFile(bucketName: String, objectPath: String) {
        try {
            // Path "fenlab-profiles/foto.jpg" veya sadece "foto.jpg" olabilir
            val fileName = if (objectPath.contains("/")) {
                objectPath.substringAfterLast("/")
            } else {
                objectPath
            }
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .build()
            )
        } catch (e: Exception) {
            throw RuntimeException("Dosya silinirken hata oluştu: ${e.message}", e)
        }
    }

    /**
     * Video silme — DB'deki path veya eski tam URL ile çalışır
     */
    fun deleteVideo(videoPath: String) {
        val fileName = videoPath.substringAfterLast("/")
        deleteFile(minioProperties.buckets.videos, fileName)
    }

    /**
     * Resim silme — DB'deki path veya eski tam URL ile çalışır
     */
    fun deleteImage(imagePath: String) {
        val fileName = imagePath.substringAfterLast("/")
        deleteFile(minioProperties.buckets.images, fileName)
    }

    /**
     * Dosya indirme
     */
    fun downloadFile(bucketName: String, fileName: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .build()
        )
    }

    private fun generateFileName(originalFileName: String, extension: String): String {
        val uuid = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        return "${timestamp}_${uuid}.$extension"
    }
}