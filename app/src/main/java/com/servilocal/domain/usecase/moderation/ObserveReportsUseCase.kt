package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.repository.ModerationRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveReportsUseCase @Inject constructor(
    private val moderationRepository: ModerationRepository
) {
    operator fun invoke(): StateFlow<List<ModerationReportItem>> = moderationRepository.reports
}
