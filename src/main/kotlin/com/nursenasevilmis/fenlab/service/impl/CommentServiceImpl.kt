package com.nursenasevilmis.fenlab.service.impl

import com.nursenasevilmis.fenlab.dto.request.CommentCreateRequestDTO
import com.nursenasevilmis.fenlab.dto.request.CommentUpdateRequestDTO
import com.nursenasevilmis.fenlab.dto.response.CommentResponseDTO
import com.nursenasevilmis.fenlab.dto.response.PaginatedResponseDTO
import com.nursenasevilmis.fenlab.exception.ForbiddenException
import com.nursenasevilmis.fenlab.exception.ResourceNotFoundException
import com.nursenasevilmis.fenlab.mapper.CommentMapper
import com.nursenasevilmis.fenlab.model.Comment
import com.nursenasevilmis.fenlab.repository.CommentRepository
import com.nursenasevilmis.fenlab.repository.ExperimentRepository
import com.nursenasevilmis.fenlab.repository.UserRepository
import com.nursenasevilmis.fenlab.service.CommentService
import com.nursenasevilmis.fenlab.service.NotificationService
import com.nursenasevilmis.fenlab.service.UserService
import com.nursenasevilmis.fenlab.util.PaginationUtils
import com.nursenasevilmis.fenlab.util.SecurityUtils
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class CommentServiceImpl(
    private val commentRepository: CommentRepository,
    private val experimentRepository: ExperimentRepository,
    private val userRepository: UserRepository,
    private val commentMapper: CommentMapper,
    private val notificationService: NotificationService
) : CommentService {

    override fun getExperimentComments(experimentId: Long, page: Int, size: Int): PaginatedResponseDTO<CommentResponseDTO> {
        if (!experimentRepository.existsById(experimentId)) {
            throw ResourceNotFoundException("Deney bulunamadı: $experimentId")
        }

        val pageable = PaginationUtils.createPageable(page, size)
        val commentsPage = commentRepository.findByExperimentId(experimentId, pageable)

        val currentUserId = try {
            SecurityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        val responses = commentsPage.content.map { comment ->
            commentMapper.toCommentResponse(comment, currentUserId)
        }

        return PaginatedResponseDTO(
            content = responses,
            page = commentsPage.number,
            size = commentsPage.size,
            totalElements = commentsPage.totalElements,
            totalPages = commentsPage.totalPages,
            isFirst = commentsPage.isFirst,
            isLast = commentsPage.isLast
        )
    }

    @Transactional
    override fun addComment(experimentId: Long, request: CommentCreateRequestDTO): CommentResponseDTO {
       val currentUserId = SecurityUtils.getCurrentUserId()

        val experiment = experimentRepository.findById(experimentId)
            .orElseThrow { ResourceNotFoundException("Deney bulunamadı: $experimentId") }

        val user = userRepository.findById(currentUserId)
            .orElseThrow { ResourceNotFoundException("Kullanıcı bulunamadı") }

        val comment = Comment(
            experiment = experiment,
            user = user,
            content = request.content,
            createdAt = LocalDateTime.now()
        )

        val savedComment = commentRepository.save(comment)

        // Bildirim oluştur (deney sahibine)
        if (experiment.user.id != currentUserId) {
            notificationService.createCommentNotification(experiment, user)
        }

        return commentMapper.toCommentResponse(savedComment, currentUserId)
    }

    @Transactional
    override fun updateComment(commentId: Long, request: CommentUpdateRequestDTO): CommentResponseDTO {
        val currentUserId = SecurityUtils.getCurrentUserId()

        val comment = commentRepository.findById(commentId)
            .orElseThrow { ResourceNotFoundException("Yorum bulunamadı: $commentId") }

        if (comment.user.id != currentUserId) {
            throw ForbiddenException("Sadece kendi yorumunuzu güncelleyebilirsiniz")
        }

        val updatedComment = comment.copy(
            content = request.content,
            updatedAt = LocalDateTime.now()
        )

        val savedComment = commentRepository.save(updatedComment)

        return commentMapper.toCommentResponse(savedComment, currentUserId)
    }

    @Transactional
    override fun deleteComment(commentId: Long) {
        val currentUserId = SecurityUtils.getCurrentUserId()

        val comment = commentRepository.findById(commentId)
            .orElseThrow { ResourceNotFoundException("Yorum bulunamadı: $commentId") }

        if (comment.user.id != currentUserId) {
            throw ForbiddenException("Sadece kendi yorumunuzu silebilirsiniz")
        }

        // Soft delete
        val deletedComment = comment.copy(isDeleted = true)
        commentRepository.save(deletedComment)
    }
}