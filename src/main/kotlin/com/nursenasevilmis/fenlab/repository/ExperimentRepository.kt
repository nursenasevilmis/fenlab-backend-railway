package com.nursenasevilmis.fenlab.repository

import com.nursenasevilmis.fenlab.model.Experiment
import com.nursenasevilmis.fenlab.model.enums.DifficultyLevel
import com.nursenasevilmis.fenlab.model.enums.SubjectType
import com.nursenasevilmis.fenlab.model.enums.EnvironmentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ExperimentRepository : JpaRepository<Experiment, Long> {

    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.isPublished = true 
        AND e.isDeleted = false
    """)
    fun findAllPublished(pageable: Pageable): Page<Experiment>

    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.user.id = :userId 
        AND e.isDeleted = false
    """)
    fun findByUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Experiment>

    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.isPublished = true 
        AND e.isDeleted = false
        AND (:subject IS NULL OR e.subject = :subject)
        AND (:environment IS NULL OR e.environment = :environment)
        AND (:difficulty IS NULL OR e.difficulty = :difficulty)
        AND (:minGradeLevel IS NULL OR e.gradeLevel >= :minGradeLevel)
        AND (:maxGradeLevel IS NULL OR e.gradeLevel <= :maxGradeLevel)
        AND (CAST(:search AS string) IS NULL 
             OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) 
             OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(e.topic) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    """)
    fun findByFilters(
        @Param("subject")       subject: SubjectType?,
        @Param("environment")   environment: EnvironmentType?,
        @Param("minGradeLevel") minGradeLevel: Int?,
        @Param("maxGradeLevel") maxGradeLevel: Int?,
        @Param("difficulty")    difficulty: DifficultyLevel?,
        @Param("search")        search: String?,
        pageable: Pageable
    ): Page<Experiment>

    // En Popüler: favori sayısına göre sırala
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.isPublished = true 
        AND e.isDeleted = false
        AND (:subject IS NULL OR e.subject = :subject)
        AND (:environment IS NULL OR e.environment = :environment)
        AND (:difficulty IS NULL OR e.difficulty = :difficulty)
        AND (:minGradeLevel IS NULL OR e.gradeLevel >= :minGradeLevel)
        AND (:maxGradeLevel IS NULL OR e.gradeLevel <= :maxGradeLevel)
        AND (CAST(:search AS string) IS NULL 
             OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) 
             OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(e.topic) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY (SELECT COUNT(f) FROM Favorite f WHERE f.experiment = e) DESC
    """)
    fun findByFiltersOrderByFavoriteCount(
        @Param("subject")       subject: SubjectType?,
        @Param("environment")   environment: EnvironmentType?,
        @Param("minGradeLevel") minGradeLevel: Int?,
        @Param("maxGradeLevel") maxGradeLevel: Int?,
        @Param("difficulty")    difficulty: DifficultyLevel?,
        @Param("search")        search: String?,
        pageable: Pageable
    ): Page<Experiment>

    // En Beğenilen: ortalama puana göre sırala
    @Query("""
        SELECT e FROM Experiment e 
        WHERE e.isPublished = true 
        AND e.isDeleted = false
        AND (:subject IS NULL OR e.subject = :subject)
        AND (:environment IS NULL OR e.environment = :environment)
        AND (:difficulty IS NULL OR e.difficulty = :difficulty)
        AND (:minGradeLevel IS NULL OR e.gradeLevel >= :minGradeLevel)
        AND (:maxGradeLevel IS NULL OR e.gradeLevel <= :maxGradeLevel)
        AND (CAST(:search AS string) IS NULL 
             OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) 
             OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(e.topic) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY (SELECT COALESCE(AVG(r.rating), 0) FROM Rating r WHERE r.experiment = e) DESC
    """)
    fun findByFiltersOrderByAverageRating(
        @Param("subject")       subject: SubjectType?,
        @Param("environment")   environment: EnvironmentType?,
        @Param("minGradeLevel") minGradeLevel: Int?,
        @Param("maxGradeLevel") maxGradeLevel: Int?,
        @Param("difficulty")    difficulty: DifficultyLevel?,
        @Param("search")        search: String?,
        pageable: Pageable
    ): Page<Experiment>

    @Query("""
        SELECT DISTINCT e.subject FROM Experiment e 
        WHERE e.isPublished = true 
        AND e.isDeleted = false
        ORDER BY e.subject
    """)
    fun findAllSubjects(): List<SubjectType>

    fun countByUserId(userId: Long): Long
}