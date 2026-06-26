package dev.wucheng.resource_viewer.ui.screens.home

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.ui.base.UiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockResourceRepo: ResourceRepository
    private lateinit var mockTagRepo: TagRepository

    private val testTag1 = Tag(
        id = "tag1",
        name = "热血",
        color = "#E53935",
        isBuiltIn = false,
        resourceCount = 2,
        createdAt = 1000L,
        updatedAt = 1000L,
    )
    private val testTag2 = Tag(
        id = "tag2",
        name = "冒险",
        color = "#1E88E5",
        isBuiltIn = false,
        resourceCount = 1,
        createdAt = 2000L,
        updatedAt = 2000L,
    )
    private val testResource1 = Resource(
        id = "res1",
        sourceId = "src1",
        sourceName = "本地",
        name = "漫画A",
        type = ResourceType.FOLDER,
        organizationMode = OrganizationMode.GALLERY,
        relativePath = "/comics/a",
        thumbnailPath = null,
        fileCount = 10,
        fileSize = 1000L,
        isAvailable = true,
        lastScannedAt = null,
        tags = listOf(testTag1),
        createdAt = 1000L,
        updatedAt = 1000L,
    )
    private val testResource2 = Resource(
        id = "res2",
        sourceId = "src1",
        sourceName = "本地",
        name = "漫画B",
        type = ResourceType.PDF,
        organizationMode = null,
        relativePath = "/comics/b.pdf",
        thumbnailPath = null,
        fileCount = null,
        fileSize = 2000L,
        isAvailable = true,
        lastScannedAt = null,
        tags = listOf(testTag1, testTag2),
        createdAt = 2000L,
        updatedAt = 2000L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockResourceRepo = mockk()
        mockTagRepo = mockk()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should emit loading then success state when resources loaded`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)
        advanceUntilIdle()

        assertEquals(UiState.SUCCESS, viewModel.uiState.value)
    }

    @Test
    fun `should emit resources list when loaded`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)
        advanceUntilIdle()

        val resources = viewModel.resources.value
        assertEquals(2, resources.size)
        assertEquals("漫画A", resources[0].name)
        assertEquals("漫画B", resources[1].name)
    }

    @Test
    fun `should toggle tag selection when selectTag called`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)
        advanceUntilIdle()

        viewModel.selectTag("tag1")
        assertEquals(setOf("tag1"), viewModel.selectedTagIds.value)

        // 再次点击取消选择
        viewModel.selectTag("tag1")
        assertEquals(emptySet<String>(), viewModel.selectedTagIds.value)
    }

    @Test
    fun `should clear filter when clearFilter called`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)
        advanceUntilIdle()

        viewModel.selectTag("tag1")
        viewModel.selectTag("tag2")
        assertEquals(setOf("tag1", "tag2"), viewModel.selectedTagIds.value)

        viewModel.clearFilter()
        assertEquals(emptySet<String>(), viewModel.selectedTagIds.value)
    }

    @Test
    fun `should emit tags list from repository`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(emptyList())
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)

        // stateIn(WhileSubscribed) 需要订阅者才能开始收集
        // 先通过 collect 触发订阅，然后检查值
        val job = launch {
            viewModel.tags.collect { /* 触发订阅 */ }
        }
        advanceUntilIdle()

        val tags = viewModel.tags.value
        assertEquals(2, tags.size)
        assertEquals("热血", tags[0].name)
        assertEquals("冒险", tags[1].name)
        job.cancel()
    }

    @Test
    fun `should filter resources by selected tags`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))
        every { mockResourceRepo.filterByTags(any()) } returns flowOf(listOf(testResource1, testResource2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)
        advanceUntilIdle()

        // 初始无筛选，显示全部
        assertEquals(2, viewModel.resources.value.size)

        // 选择 tag1，两个资源都有 tag1
        viewModel.selectTag("tag1")
        advanceUntilIdle()
        val filtered = viewModel.resources.value
        // filterByTags 应该返回两个资源（都有 tag1）
        assertEquals(2, filtered.size)
    }

    @Test
    fun `should emit empty resources when repository returns empty`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(emptyList())
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo)
        advanceUntilIdle()

        assertEquals(UiState.SUCCESS, viewModel.uiState.value)
        assertTrue(viewModel.resources.value.isEmpty())
    }
}
