package com.nursenasevilmis.fenlab.service.impl

import com.nursenasevilmis.fenlab.dto.response.FileUploadResponseDTO
import com.nursenasevilmis.fenlab.service.CloudinaryService
import com.nursenasevilmis.fenlab.service.FileUploadService
import com.nursenasevilmis.fenlab.service.MinioService
import com.nursenasevilmis.fenlab.util.FileUtils
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
@Service
class FileUploadServiceImpl(
    private val cloudinaryService: CloudinaryService
) : FileUploadService {

    override fun uploadVideo(file: MultipartFile): FileUploadResponseDTO {
        FileUtils.validateVideoFile(file)
        val url = cloudinaryService.uploadVideo(file)
        return FileUploadResponseDTO(
            fileName = file.originalFilename ?: "video",
            fileUrl = url,
            fileSize = file.size,
            contentType = file.contentType ?: "video/mp4"
        )
    }

    override fun uploadImage(file: MultipartFile): FileUploadResponseDTO {
        FileUtils.validateImageFile(file)
        val url = cloudinaryService.uploadImage(file)
        return FileUploadResponseDTO(
            fileName = file.originalFilename ?: "image",
            fileUrl = url,
            fileSize = file.size,
            contentType = file.contentType ?: "image/jpeg"
        )
    }

    override fun uploadProfileImage(file: MultipartFile): FileUploadResponseDTO {
        FileUtils.validateImageFile(file)
        val url = cloudinaryService.uploadProfileImage(file)
        return FileUploadResponseDTO(
            fileName = file.originalFilename ?: "profile",
            fileUrl = url,
            fileSize = file.size,
            contentType = file.contentType ?: "image/jpeg"
        )
    }

    override fun deleteVideo(videoUrl: String) {}
    override fun deleteImage(imageUrl: String) {}
}