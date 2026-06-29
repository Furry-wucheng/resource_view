package dev.wucheng.resource_viewer.ui.screens.home

import dev.wucheng.resource_viewer.data.local.converter.OrganizationMode
import dev.wucheng.resource_viewer.data.local.converter.ResourceType
import dev.wucheng.resource_viewer.data.local.dao.ResourceTagDao
import dev.wucheng.resource_viewer.data.local.datastore.HomePrefsStore
import dev.wucheng.resource_viewer.data.repository.ResourceRepository
import dev.wucheng.resource_viewer.data.repository.TagRepository
import dev.wucheng.resource_viewer.domain.model.Resource
import dev.wucheng.resource_viewer.domain.model.Tag
import dev.wucheng.resource_viewer.ui.base.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockResourceRepo: ResourceRepository
    private lateinit var mockTagRepo: TagRepository
    private lateinit var mockResourceTagDao: ResourceTagDao
    private lateinit var mockHomePrefsStore: HomePrefsStore

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
        mockResourceTagDao = mockk()
        mockHomePrefsStore = mockk()
        coEvery { mockHomePrefsStore.loadResourceSort() } returns null
        coEvery { mockHomePrefsStore.saveResourceSort(any()) } just Runs
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should emit loading then success state when resources loaded`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        assertEquals(UiState.SUCCESS, viewModel.uiState.value)
    }

    @Test
    fun `should emit resources list when loaded`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
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

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
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

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
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

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)

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

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        // 初始无筛选，显示全部
        assertEquals(2, viewModel.resources.value.size)

        // 选择 tag1，两个资源都有 tag1
        viewModel.selectTag("tag1")
        advanceUntilIdle()
        val filtered = viewModel.resources.value
        assertEquals(2, filtered.size)
    }

    @Test
    fun `should emit empty resources when repository returns empty`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(emptyList())
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        assertEquals(UiState.SUCCESS, viewModel.uiState.value)
        assertTrue(viewModel.resources.value.isEmpty())
    }

    @Test
    fun `search query filters resources by name`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())
        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        viewModel.setSearchQuery("A")
        advanceUntilIdle()
        assertEquals(listOf("漫画A"), viewModel.resources.value.map { it.name })
    }

    @Test
    fun `name descending sort reorders visible resources`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())
        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        viewModel.setSort(HomeViewModel.ResourceSort.NAME_DESC)
        advanceUntilIdle()
        assertEquals(listOf("漫画B", "漫画A"), viewModel.resources.value.map { it.name })
    }

    // === 资源详情弹窗测试 ===

    @Test
    fun `should open resource detail with correct state`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openResourceDetail(testResource1)
        advanceUntilIdle()

        assertEquals(testResource1, viewModel.detailResource.value)
        assertEquals(setOf("tag1"), viewModel.detailTagIds.value)
        assertEquals(OrganizationMode.GALLERY, viewModel.detailOrgMode.value)
    }

    @Test
    fun `should toggle detail tag`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openResourceDetail(testResource1)
        advanceUntilIdle()

        // tag1 is already selected, toggle it off
        viewModel.toggleDetailTag("tag1")
        assertEquals(emptySet<String>(), viewModel.detailTagIds.value)

        // Toggle tag2 on
        viewModel.toggleDetailTag("tag2")
        assertEquals(setOf("tag2"), viewModel.detailTagIds.value)
    }

    @Test
    fun `should set detail org mode`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openResourceDetail(testResource1)
        advanceUntilIdle()

        viewModel.setDetailOrgMode(OrganizationMode.CHAPTER)
        assertEquals(OrganizationMode.CHAPTER, viewModel.detailOrgMode.value)
    }

    @Test
    fun `should close resource detail`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openResourceDetail(testResource1)
        advanceUntilIdle()

        viewModel.closeResourceDetail()
        advanceUntilIdle()

        assertEquals(null, viewModel.detailResource.value)
        assertEquals(emptySet<String>(), viewModel.detailTagIds.value)
    }

    @Test
    fun `should save selected organization mode when saving resource detail`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))
        coEvery { mockResourceRepo.updateOrganizationMode(any(), any()) } returns dev.wucheng.resource_viewer.domain.error.Result.Ok(Unit)
        coEvery { mockResourceTagDao.deleteByResourceId(any()) } just Runs
        coEvery { mockResourceTagDao.insertAll(any()) } just Runs

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openResourceDetail(testResource1)
        viewModel.setDetailOrgMode(OrganizationMode.CHAPTER)
        viewModel.saveResourceDetail()
        advanceUntilIdle()

        coVerify { mockResourceRepo.updateOrganizationMode("res1", OrganizationMode.CHAPTER) }
    }

    // === 自然排序测试 ===

    private val testResource3 = Resource(
        id = "res3", sourceId = "src1", sourceName = "本地", name = "漫画10",
        type = ResourceType.FOLDER, organizationMode = OrganizationMode.GALLERY,
        relativePath = "/comics/c10", thumbnailPath = null,
        fileCount = 10, fileSize = 3000L, isAvailable = true,
        lastScannedAt = null, tags = emptyList(),
        createdAt = 3000L, updatedAt = 3000L,
    )
    private val testResource11 = Resource(
        id = "res11", sourceId = "src1", sourceName = "本地", name = "漫画2",
        type = ResourceType.FOLDER, organizationMode = OrganizationMode.GALLERY,
        relativePath = "/comics/c2", thumbnailPath = null,
        fileCount = 10, fileSize = 3000L, isAvailable = true,
        lastScannedAt = null, tags = emptyList(),
        createdAt = 4000L, updatedAt = 4000L,
    )
    private val testResource12 = Resource(
        id = "res12", sourceId = "src1", sourceName = "本地", name = "漫画1",
        type = ResourceType.FOLDER, organizationMode = OrganizationMode.GALLERY,
        relativePath = "/comics/c1", thumbnailPath = null,
        fileCount = 10, fileSize = 3000L, isAvailable = true,
        lastScannedAt = null, tags = emptyList(),
        createdAt = 5000L, updatedAt = 5000L,
    )

    @Test
    fun `should sort resources naturally by name ascending`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource3, testResource11, testResource12))
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        viewModel.setSort(HomeViewModel.ResourceSort.NAME_ASC)
        advanceUntilIdle()

        assertEquals(listOf("漫画1", "漫画2", "漫画10"), viewModel.resources.value.map { it.name })
    }

    @Test
    fun `should sort resources naturally by name descending`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource3, testResource11, testResource12))
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        viewModel.setSort(HomeViewModel.ResourceSort.NAME_DESC)
        advanceUntilIdle()

        assertEquals(listOf("漫画10", "漫画2", "漫画1"), viewModel.resources.value.map { it.name })
    }

    // === 排序持久化测试 ===

    @Test
    fun `should persist sort mode when setSort called`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(emptyList())
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        viewModel.setSort(HomeViewModel.ResourceSort.NAME_DESC)
        advanceUntilIdle()

        coVerify { mockHomePrefsStore.saveResourceSort(HomeViewModel.ResourceSort.NAME_DESC.name) }
    }

    @Test
    fun `should load saved sort mode on init`() = runTest {
        coEvery { mockHomePrefsStore.loadResourceSort() } returns "NAME_DESC"
        every { mockResourceRepo.getVisibleResources() } returns flowOf(emptyList())
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        assertEquals(HomeViewModel.ResourceSort.NAME_DESC, viewModel.sort.value)
    }

    @Test
    fun `should fall back to default sort when saved value is invalid`() = runTest {
        coEvery { mockHomePrefsStore.loadResourceSort() } returns "INVALID_SORT"
        every { mockResourceRepo.getVisibleResources() } returns flowOf(emptyList())
        every { mockTagRepo.getAllTags() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        assertEquals(HomeViewModel.ResourceSort.ADDED_ASC, viewModel.sort.value)
    }

    // === 批量添加标签测试 ===

    @Test
    fun `should show batch tag dialog when openBatchTagDialog called`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openBatchTagDialog()
        advanceUntilIdle()

        assertTrue(viewModel.showBatchTagDialog.value)
        assertEquals(emptySet<String>(), viewModel.batchTagSelectedIds.value)
    }

    @Test
    fun `should hide batch tag dialog when hideBatchTagDialog called`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openBatchTagDialog()
        viewModel.hideBatchTagDialog()
        advanceUntilIdle()

        assertFalse(viewModel.showBatchTagDialog.value)
    }

    @Test
    fun `should toggle batch tag selection`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openBatchTagDialog()
        viewModel.toggleBatchTag("tag1")
        assertEquals(setOf("tag1"), viewModel.batchTagSelectedIds.value)

        viewModel.toggleBatchTag("tag1")
        assertEquals(emptySet<String>(), viewModel.batchTagSelectedIds.value)
    }

    @Test
    fun `batchAddTags should apply tags to selected resources`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1, testResource2))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))
        coEvery { mockResourceTagDao.deleteByResourceId(any()) } just Runs
        coEvery { mockResourceTagDao.insertAll(any()) } just Runs

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.enterMultiSelectMode()
        viewModel.toggleResourceSelection("res1")
        viewModel.toggleResourceSelection("res2")
        viewModel.openBatchTagDialog()
        viewModel.toggleBatchTag("tag2")

        viewModel.batchAddTags()
        advanceUntilIdle()

        assertFalse(viewModel.showBatchTagDialog.value)
        coVerify { mockResourceTagDao.deleteByResourceId("res1") }
        coVerify { mockResourceTagDao.deleteByResourceId("res2") }
        coVerify { mockResourceTagDao.insertAll(any()) }
    }

    @Test
    fun `deleteDetailResource should delete and close detail`() = runTest {
        every { mockResourceRepo.getVisibleResources() } returns flowOf(listOf(testResource1))
        every { mockTagRepo.getAllTags() } returns flowOf(listOf(testTag1, testTag2))
        coEvery { mockResourceRepo.deleteById(any()) } returns dev.wucheng.resource_viewer.domain.error.Result.Ok(Unit)

        val viewModel = HomeViewModel(mockResourceRepo, mockTagRepo, mockResourceTagDao, mockHomePrefsStore)
        advanceUntilIdle()

        viewModel.openResourceDetail(testResource1)
        viewModel.deleteDetailResource()
        advanceUntilIdle()

        coVerify { mockResourceRepo.deleteById("res1") }
        assertEquals(null, viewModel.detailResource.value)
    }
}
