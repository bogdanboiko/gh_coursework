package com.example.gh_coursework.domain.usecase

import com.example.gh_coursework.domain.entity.PointDetailsDomain
import com.example.gh_coursework.domain.repository.TravelRepository
import kotlinx.coroutines.flow.Flow

class GetPointDetailsUseCaseImpl(private val repository: TravelRepository) : GetPointDetailsUseCase {
    override fun invoke(pointId: Int): Flow<PointDetailsDomain> {
       return repository.getPointOfInterestDetails(pointId)
    }
}