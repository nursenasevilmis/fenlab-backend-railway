package com.nursenasevilmis.fenlab.service

import com.nursenasevilmis.fenlab.dto.response.FileUploadResponseDTO
import org.springframework.web.multipart.MultipartFile

interface FileUploadService {
    fun uploadVideo(file: MultipartFile): FileUploadResponseDTO
    fun uploadImage(file: MultipartFile): FileUploadResponseDTO
    fun uploadProfileImage(file: MultipartFile): FileUploadResponseDTO
    fun deleteVideo(videoUrl: String)
    fun deleteImage(imageUrl: String)
    fun getPublicFile(bucket: String, fileName: String): ByteArray
}