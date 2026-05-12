package com.nursenasevilmis.fenlab.service.impl

import com.nursenasevilmis.fenlab.dto.request.*
import com.nursenasevilmis.fenlab.dto.response.*
import com.nursenasevilmis.fenlab.exception.ForbiddenException
import com.nursenasevilmis.fenlab.exception.ResourceNotFoundException
import com.nursenasevilmis.fenlab.mapper.ExperimentMapper
import com.nursenasevilmis.fenlab.model.*
import com.nursenasevilmis.fenlab.model.enums.*
import com.nursenasevilmis.fenlab.repository.*
import com.nursenasevilmis.fenlab.service.ExperimentService
import com.nursenasevilmis.fenlab.util.PaginationUtils
import com.nursenasevilmis.fenlab.util.SecurityUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExperimentServiceImpl(
    private val experimentRepository: ExperimentRepository,
    private val experimentMaterialRepository: ExperimentMaterialRepository,
    private val experimentStepRepository: ExperimentStepRepository,
    private val experimentMediaRepository: ExperimentMediaRepository,
    private val userRepository: UserRepository,
    private val experimentMapper: ExperimentMapper
) : ExperimentService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager


    override fun getAllExperiments(filterRequest: ExperimentFilterRequestDTO): PaginatedResponseDTO<ExperimentSummaryResponseDTO> {

        // Favori sayısı ve puana göre sıralamalar özel sorgu gerektirir (subquery)
        // Diğerleri standard Sort ile findByFilters kullanır
        val page = when (filterRequest.sortType) {
            SortType.MOST_FAVORITED -> {
                val pageable = PaginationUtils.createPageable(filterRequest.page, filterRequest.size)
                experimentRepository.findByFiltersOrderByFavoriteCount(
                    subject       = filterRequest.subject,
                    environment   = filterRequest.environment,
                    minGradeLevel = filterRequest.minGradeLevel,
                    maxGradeLevel = filterRequest.maxGradeLevel,
                    difficulty    = filterRequest.difficulty,
                    search        = filterRequest.search,
                    pageable      = pageable
                )
            }
            SortType.HIGHEST_RATED -> {
                val pageable = PaginationUtils.createPageable(filterRequest.page, filterRequest.size)
                experimentRepository.findByFiltersOrderByAverageRating(
                    subject       = filterRequest.subject,
                    environment   = filterRequest.environment,
                    minGradeLevel = filterRequest.minGradeLevel,
                    maxGradeLevel = filterRequest.maxGradeLevel,
                    difficulty    = filterRequest.difficulty,
                    search        = filterRequest.search,
                    pageable      = pageable
                )
            }
            else -> {
                val sort = when (filterRequest.sortType) {
                    SortType.MOST_RECENT -> Sort.by(Sort.Direction.DESC, "createdAt")
                    SortType.OLDEST      -> Sort.by(Sort.Direction.ASC,  "createdAt")
                    else                 -> Sort.by(Sort.Direction.DESC, "createdAt")
                }
                val pageable = PaginationUtils.createPageable(filterRequest.page, filterRequest.size, sort)
                experimentRepository.findByFilters(
                    subject       = filterRequest.subject,
                    environment   = filterRequest.environment,
                    minGradeLevel = filterRequest.minGradeLevel,
                    maxGradeLevel = filterRequest.maxGradeLevel,
                    difficulty    = filterRequest.difficulty,
                    search        = filterRequest.search,
                    pageable      = pageable
                )
            }
        }

        val currentUserId = try {
            SecurityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        val summaries = page.content.map { experiment ->
            experimentMapper.toExperimentSummary(experiment, currentUserId)
        }

        return PaginatedResponseDTO(
            content = summaries,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isFirst = page.isFirst,
            isLast = page.isLast
        )
    }

    override fun getExperimentById(experimentId: Long): ExperimentResponseDTO {
        val experiment = experimentRepository.findById(experimentId)
            .orElseThrow { ResourceNotFoundException("Deney bulunamadı: $experimentId") }

        if (!experiment.isPublished) {
            val currentUserId = try {
                SecurityUtils.getCurrentUserId()
            } catch (e: Exception) {
                throw ForbiddenException("Bu deneye erişim izniniz yok")
            }

            if (experiment.user.id != currentUserId) {
                throw ForbiddenException("Bu deneye erişim izniniz yok")
            }
        }

        val currentUserId = try {
            SecurityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        return experimentMapper.toExperimentResponse(experiment, currentUserId)
    }

    @Transactional
    override fun createExperiment(request: ExperimentCreateRequestDTO): ExperimentResponseDTO {
        val currentUserId = SecurityUtils.getCurrentUserId()
        val user = userRepository.findById(currentUserId)
            .orElseThrow { ResourceNotFoundException("Kullanıcı bulunamadı") }

        val experiment = Experiment(
            user = user,
            title = request.title,
            description = request.description,
            gradeLevel = request.gradeLevel,
            subject = request.subject,
            environment = request.environment,
            topic = request.topic,
            difficulty = request.difficulty,
            expectedResult = request.expectedResult,
            safetyNotes = request.safetyNotes,
            isPublished = request.isPublished,
            createdAt = LocalDateTime.now()
        )

        val savedExperiment = experimentRepository.save(experiment)

        // Materials
        request.materials.forEach { materialReq ->
            experimentMaterialRepository.save(
                ExperimentMaterial(
                    experiment = savedExperiment,
                    materialName = materialReq.materialName,
                    quantity = materialReq.quantity
                )
            )
        }

        // Steps
        request.steps.forEach { stepReq ->
            experimentStepRepository.save(
                ExperimentStep(
                    experiment = savedExperiment,
                    stepOrder = stepReq.stepOrder,
                    stepText = stepReq.stepText
                )
            )
        }

        // Media
        request.media.forEach { mediaReq ->
            experimentMediaRepository.save(
                ExperimentMedia(
                    experiment = savedExperiment,
                    mediaType = mediaReq.mediaType,
                    mediaUrl = mediaReq.mediaUrl,
                    mediaOrder = mediaReq.mediaOrder
                )
            )
        }

        return getExperimentById(savedExperiment.id!!)
    }

    @Transactional
    override fun updateExperiment(experimentId: Long, request: ExperimentUpdateRequestDTO): ExperimentResponseDTO {
        val experiment = experimentRepository.findById(experimentId)
            .orElseThrow { ResourceNotFoundException("Deney bulunamadı: $experimentId") }

        val currentUserId = SecurityUtils.getCurrentUserId()

        val updatedExperiment = experiment.copy(
            title = request.title ?: experiment.title,
            description = request.description ?: experiment.description,
            gradeLevel = request.gradeLevel ?: experiment.gradeLevel,
            subject = request.subject ?: experiment.subject,
            environment = request.environment ?: experiment.environment,
            topic = request.topic ?: experiment.topic,
            difficulty = request.difficulty ?: experiment.difficulty,
            expectedResult = request.expectedResult ?: experiment.expectedResult,
            safetyNotes = request.safetyNotes ?: experiment.safetyNotes,
            isPublished = request.isPublished ?: experiment.isPublished,
            updatedAt = LocalDateTime.now()
        )

        experimentRepository.save(updatedExperiment)

        request.materials?.let {
            experimentMaterialRepository.deleteByExperimentId(experimentId)
            experimentMaterialRepository.flush()   // ← DELETE önce commit edilsin
            it.forEach { req ->
                experimentMaterialRepository.save(
                    ExperimentMaterial(
                        experiment = updatedExperiment,
                        materialName = req.materialName,
                        quantity = req.quantity
                    )
                )
            }
        }

        request.steps?.let {
            experimentStepRepository.deleteByExperimentId(experimentId)
            experimentStepRepository.flush()   // ← DELETE DB'ye yazılsın, unique constraint çakışmasın
            it.forEach { req ->
                experimentStepRepository.save(
                    ExperimentStep(
                        experiment = updatedExperiment,
                        stepOrder = req.stepOrder,
                        stepText = req.stepText
                    )
                )
            }
        }

        request.media?.let {
            experimentMediaRepository.deleteByExperimentId(experimentId)
            experimentMediaRepository.flush()   // ← DELETE önce commit edilsin
            it.forEach { req ->
                experimentMediaRepository.save(
                    ExperimentMedia(
                        experiment = updatedExperiment,
                        mediaType = req.mediaType,
                        mediaUrl = req.mediaUrl,
                        mediaOrder = req.mediaOrder
                    )
                )
            }
        }

        return getExperimentById(experimentId)
    }

    @Transactional
    override fun deleteExperiment(experimentId: Long) {
        val experiment = experimentRepository.findById(experimentId)
            .orElseThrow { ResourceNotFoundException("Deney bulunamadı: $experimentId") }

        val currentUserId = SecurityUtils.getCurrentUserId()
        val deletedExperiment = experiment.copy(isDeleted = true)
        experimentRepository.save(deletedExperiment)
    }

    override fun getUserExperiments(userId: Long, page: Int, size: Int): PaginatedResponseDTO<ExperimentSummaryResponseDTO> {
        val pageable = PaginationUtils.createPageable(page, size)
        val experimentsPage = experimentRepository.findByUserId(userId, pageable)

        val currentUserId = try {
            SecurityUtils.getCurrentUserId()
        } catch (e: Exception) {
            null
        }

        val summaries = experimentsPage.content.map { experiment ->
            experimentMapper.toExperimentSummary(experiment, currentUserId)
        }

        return PaginatedResponseDTO(
            content = summaries,
            page = experimentsPage.number,
            size = experimentsPage.size,
            totalElements = experimentsPage.totalElements,
            totalPages = experimentsPage.totalPages,
            isFirst = experimentsPage.isFirst,
            isLast = experimentsPage.isLast
        )
    }

    override fun getAllSubjects(): List<String> {
        return experimentRepository.findAllSubjects().map { it.name }
    }

    override fun mapToExperimentSummary(experiment: Experiment, currentUserId: Long?): ExperimentSummaryResponseDTO {
        return experimentMapper.toExperimentSummary(experiment, currentUserId)
    }
}