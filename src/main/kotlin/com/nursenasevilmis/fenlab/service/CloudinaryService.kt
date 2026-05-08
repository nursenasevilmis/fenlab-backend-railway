package com.nursenasevilmis.fenlab.service

import com.cloudinary.Cloudinary
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CloudinaryService(private val cloudinary: Cloudinary) {

    fun uploadImage(file: MultipartFile, folder: String = "fenlab-images"): String {
        val result = cloudinary.uploader().upload(
            file.bytes,
            mapOf("folder" to folder, "resource_type" to "image")
        )
        return result["secure_url"] as String
    }

    fun uploadVideo(file: MultipartFile, folder: String = "fenlab-videos"): String {
        val result = cloudinary.uploader().upload(
            file.bytes,
            mapOf("folder" to folder, "resource_type" to "video")
        )
        return result["secure_url"] as String
    }

    fun uploadProfileImage(file: MultipartFile): String {
        return uploadImage(file, "fenlab-profiles")
    }

    fun uploadPdf(file: MultipartFile): String {
        val result = cloudinary.uploader().upload(
            file.bytes,
            mapOf("folder" to "fenlab-pdfs", "resource_type" to "raw")
        )
        return result["secure_url"] as String
    }

    fun deleteFile(publicId: String) {
        cloudinary.uploader().destroy(publicId, mapOf("resource_type" to "image"))
    }
}