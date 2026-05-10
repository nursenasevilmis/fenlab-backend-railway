package com.nursenasevilmis.fenlab.service.impl

import com.nursenasevilmis.fenlab.dto.response.FileUploadResponseDTO
import com.nursenasevilmis.fenlab.service.FileUploadService
import com.nursenasevilmis.fenlab.service.MinioService
import com.nursenasevilmis.fenlab.util.FileUtils
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class FileUploadServiceImpl(
    private val minioService: MinioService
) : FileUploadService {

    override fun uploadVideo(file: MultipartFile): FileUploadResponseDTO {
        FileUtils.validateVideoFile(file)

        val videoUrl = minioService.uploadVideo(file)

        return FileUploadResponseDTO(
            fileName = file.originalFilename ?: "video",
            fileUrl = videoUrl,
            fileSize = file.size,
            contentType = file.contentType ?: "video/mp4"
        )
    }

    override fun uploadImage(file: MultipartFile): FileUploadResponseDTO {
        FileUtils.validateImageFile(file)

        val imageUrl = minioService.uploadImage(file)

        return FileUploadResponseDTO(
            fileName = file.originalFilename ?: "image",
            fileUrl = imageUrl,
            fileSize = file.size,
            contentType = file.contentType ?: "image/jpeg"
        )
    }

    override fun uploadProfileImage(file: MultipartFile): FileUploadResponseDTO {
        FileUtils.validateImageFile(file)

        val imageUrl = minioService.uploadProfileImage(file)

        return FileUploadResponseDTO(
            fileName = file.originalFilename ?: "profile",
            fileUrl = imageUrl,
            fileSize = file.size,
            contentType = file.contentType ?: "image/jpeg"
        )
    }

    override fun deleteVideo(videoUrl: String) {
        minioService.deleteVideo(videoUrl)
    }

    override fun deleteImage(imageUrl: String) {
        minioService.deleteImage(imageUrl)
    }
}
