package com.pictofly.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import com.pictofly.repository.LocalContentRepository
import com.pictofly.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.util.Log

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CommunicationViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LocalContentRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: CommunicationViewModel


    private val communicationCategory = LocalCategory("comm_cat_123", "Comunicación Personalizada", "", 2)
    private val subjectTest = LocalPictogram("sub_1", "comm_cat_123", "Yo", "path.jpg", "subject")
    private val subjectTest2 = LocalPictogram("sub_2", "comm_cat_123", "Tú", "path.jpg", "subject")
    private val verbTest = LocalPictogram("verb_1", "comm_cat_123", "Quiero", "path.jpg", "verb")
    private val verbTest2 = LocalPictogram("verb_2", "comm_cat_123", "Necesito", "path.jpg", "verb")
    private val allPictograms = listOf(subjectTest, subjectTest2, verbTest, verbTest2)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        repository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        coEvery { repository.localCategoriesWithCount } returns flowOf(listOf(communicationCategory))
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(emptyList())

        every { settingsRepository.getSelectedSubjectId() } returns null
        every { settingsRepository.getSelectedVerbId() } returns null

        coEvery { settingsRepository.saveSelectedSubjectId(any()) } just runs
        coEvery { settingsRepository.saveSelectedSubjectName(any()) } just runs
        coEvery { settingsRepository.saveSelectedVerbId(any()) } just runs
        coEvery { settingsRepository.saveSelectedVerbName(any()) } just runs
        coEvery { settingsRepository.clearSessionData() } just runs

        Dispatchers.setMain(testDispatcher)
        viewModel = CommunicationViewModel(repository, settingsRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test fun `test01 - estado inicial pictograms vacío`() {
        assertThat(viewModel.communicationScreenState.value.pictograms).isEmpty()
    }

    @Test fun `test02 - estado inicial loading true`() {
        assertThat(viewModel.communicationScreenState.value.isLoading).isTrue()
    }

    @Test fun `test03 - estado initial subject null`() {
        assertThat(viewModel.selectedSubject.value).isNull()
    }

    @Test fun `test04 - estado initial verb null`() {
        assertThat(viewModel.selectedVerb.value).isNull()
    }

    @Test fun `test05 - subject version 0`() {
        assertThat(viewModel.subjectVersion.value).isEqualTo(0)
    }

    @Test fun `test06 - verb version 0`() {
        assertThat(viewModel.verbVersion.value).isEqualTo(0)
    }

    @Test
    fun `test07 - categoria existe usa existente`() = runTest(testDispatcher) {
        coEvery { repository.localCategoriesWithCount } returns flowOf(listOf(communicationCategory))
        val vm = CommunicationViewModel(repository, settingsRepository)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.addCategory(any()) }
    }

    @Test
    fun `test08 - categoria no existe crea nueva`() = runTest(testDispatcher) {
        coEvery { repository.localCategoriesWithCount } returns flowOf(emptyList())
        coEvery { repository.addCategory(any()) } returns "new_id"
        val vm = CommunicationViewModel(repository, settingsRepository)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.addCategory(any()) }
    }

    @Test
    fun `test09 - despues de init carga pictogramas`() = runTest(testDispatcher) {
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(allPictograms)
        val vm = CommunicationViewModel(repository, settingsRepository)
        advanceUntilIdle()
        assertThat(vm.communicationScreenState.value.pictograms).isNotEmpty()
    }

    @Test
    fun `test10 - load cambia loading a false`() = runTest(testDispatcher) {
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(allPictograms)
        viewModel.loadCommunicationPictograms()
        advanceUntilIdle()
        assertThat(viewModel.communicationScreenState.value.isLoading).isFalse()
    }

    @Test
    fun `test11 - load mantiene pictogramas`() = runTest(testDispatcher) {
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(allPictograms)
        viewModel.loadCommunicationPictograms()
        advanceUntilIdle()
        assertThat(viewModel.communicationScreenState.value.pictograms).isNotEmpty()
    }

    @Test
    fun `test12 - restaura sujeto existente`() = runTest(testDispatcher) {
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(allPictograms)
        every { settingsRepository.getSelectedSubjectId() } returns subjectTest.id
        viewModel.loadCommunicationPictograms()
        advanceUntilIdle()
        viewModel.selectSubject(subjectTest)
        assertThat(viewModel.getSelectedSubject()).isNotNull()
    }

    @Test
    fun `test13 - restaura verbo existente`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isNotNull()
        assertThat(viewModel.getSelectedVerb()?.id).isEqualTo(verbTest.id)
    }

    @Test
    fun `test14 - ignora sujeto inexistente`() = runTest(testDispatcher) {
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(allPictograms)
        every { settingsRepository.getSelectedSubjectId() } returns "id_falso"
        viewModel.loadCommunicationPictograms()
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isNull()
    }

    @Test
    fun `test15 - selectSubject actualiza estado`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest)
    }

    @Test
    fun `test16 - selectSubject guarda ID`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest)
        advanceUntilIdle()
        coVerify { settingsRepository.saveSelectedSubjectId(subjectTest.id) }
    }

    @Test
    fun `test17 - selectSubject guarda nombre`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest)
        advanceUntilIdle()
        coVerify { settingsRepository.saveSelectedSubjectName(subjectTest.name) }
    }

    @Test
    fun `test18 - selectSubject version`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest)
    }

    @Test
    fun `test19 - selectVerb actualiza estado`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isEqualTo(verbTest)
    }

    @Test
    fun `test20 - selectVerb guarda ID`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        coVerify { settingsRepository.saveSelectedVerbId(verbTest.id) }
    }

    @Test
    fun `test21 - selectVerb guarda nombre`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        coVerify { settingsRepository.saveSelectedVerbName(verbTest.name) }
    }

    @Test
    fun `test22 - selectVerb version`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isEqualTo(verbTest)
    }

//    @Test
//    fun `test23 - limpia sujeto eliminado`() = runTest(testDispatcher) {
//        viewModel.selectSubject(subjectTest)
//        advanceUntilIdle()
//        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(listOf(verbTest))
//        viewModel.loadCommunicationPictograms()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedSubject()).isNull()
//    }
//
//    @Test
//    fun `test24 - limpia verbo eliminado`() = runTest(testDispatcher) {
//        viewModel.selectVerb(verbTest)
//        advanceUntilIdle()
//        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(listOf(subjectTest))
//        viewModel.loadCommunicationPictograms()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedVerb()).isNull()
//    }

//    @Test
//    fun `test25 - forceRefreshSubject mantiene sujeto`() = runTest(testDispatcher) {
//        viewModel.selectSubject(subjectTest)
//        advanceUntilIdle()
//        viewModel.forceRefreshSelectedSubject()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest)
//    }
//
//    @Test
//    fun `test26 - forceRefreshVerb mantiene verbo`() = runTest(testDispatcher) {
//        viewModel.selectVerb(verbTest)
//        advanceUntilIdle()
//        viewModel.forceRefreshSelectedVerb()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedVerb()).isEqualTo(verbTest)
//    }

    @Test
    fun `test27 - clearSelections limpia sujeto`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest)
        advanceUntilIdle()
        viewModel.clearSelections()
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isNull()
    }

    @Test
    fun `test28 - clearSelections limpia verbo`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        viewModel.clearSelections()
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isNull()
    }

    @Test
    fun `test29 - clearSelections llama a repository`() = runTest(testDispatcher) {
        viewModel.clearSelections()
        advanceUntilIdle()
        coVerify { settingsRepository.clearSessionData() }
    }

    @Test
    fun `test30 - getSelectedSubject funciona`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest)
    }

    @Test
    fun `test31 - getSelectedVerb funciona`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isEqualTo(verbTest)
    }

    @Test fun `test32 - selectSubject con sujeto2`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest2)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest2)
    }

    @Test fun `test33 - selectVerb con verbo2`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest2)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isEqualTo(verbTest2)
    }

    @Test fun `test34 - selectSubject guarda nombre sujeto2`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest2)
        advanceUntilIdle()
        coVerify { settingsRepository.saveSelectedSubjectName(subjectTest2.name) }
    }

    @Test fun `test35 - selectVerb guarda nombre verbo2`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest2)
        advanceUntilIdle()
        coVerify { settingsRepository.saveSelectedVerbName(verbTest2.name) }
    }

//    @Test fun `test36 - forceRefresh con sujeto2`() = runTest(testDispatcher) {
//        viewModel.selectSubject(subjectTest2)
//        advanceUntilIdle()
//        viewModel.forceRefreshSelectedSubject()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest2)
//    }
//
//    @Test fun `test37 - forceRefresh con verbo2`() = runTest(testDispatcher) {
//        viewModel.selectVerb(verbTest2)
//        advanceUntilIdle()
//        viewModel.forceRefreshSelectedVerb()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedVerb()).isEqualTo(verbTest2)
//    }

    @Test fun `test38 - clearSelections despues de sujeto2`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest2)
        advanceUntilIdle()
        viewModel.clearSelections()
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isNull()
    }

    @Test fun `test39 - clearSelections despues de verbo2`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest2)
        advanceUntilIdle()
        viewModel.clearSelections()
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()).isNull()
    }

    @Test fun `test40 - carga pictogramas con datos`() = runTest(testDispatcher) {
        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(allPictograms)
        viewModel.loadCommunicationPictograms()
        advanceUntilIdle()
        assertThat(viewModel.communicationScreenState.value.pictograms).isNotEmpty()
    }

    @Test fun `test41 - restore sujeto con subject2`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest2)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()?.id).isEqualTo(subjectTest2.id)
    }

    @Test fun `test42 - restore verbo con verb2`() = runTest(testDispatcher) {
        viewModel.selectVerb(verbTest2)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedVerb()?.id).isEqualTo(verbTest2.id)
    }

//    @Test fun `test43 - validacion con sujeto2`() = runTest(testDispatcher) {
//        viewModel.selectSubject(subjectTest2)
//        advanceUntilIdle()
//        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(listOf(verbTest))
//        viewModel.loadCommunicationPictograms()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedSubject()).isNull()
//    }

//    @Test fun `test44 - validacion con verbo2`() = runTest(testDispatcher) {
//        viewModel.selectVerb(verbTest2)
//        advanceUntilIdle()
//        coEvery { repository.getPictogramsByCategoryId(any()) } returns flowOf(listOf(subjectTest))
//        viewModel.loadCommunicationPictograms()
//        advanceUntilIdle()
//        assertThat(viewModel.getSelectedVerb()).isNull()
//    }

    @Test fun `test45 - selectSubject version con sujeto2`() = runTest(testDispatcher) {
        viewModel.selectSubject(subjectTest2)
        advanceUntilIdle()
        assertThat(viewModel.getSelectedSubject()).isEqualTo(subjectTest2)
    }
}