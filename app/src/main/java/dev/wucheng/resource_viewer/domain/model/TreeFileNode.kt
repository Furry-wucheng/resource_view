package dev.wucheng.resource_viewer.domain.model

/**
 * 树形文件节点。
 * 用于 ResourcePicker 弹窗中展示可勾选的文件树。
 *
 * @param name 显示名称
 * @param relativePath 相对路径（用于 FileSource 调用）
 * @param isDirectory 是否为目录
 * @param children 子节点列表
 * @param isExpanded 是否展开（仅目录有效）
 * @param isChecked 是否勾选
 * @param isExpandable 是否可展开（混合内容目录可展开，纯图片目录不可展开）
 * @param fileCount 子文件数量（仅目录有效，用于显示）
 */
data class TreeFileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val children: List<TreeFileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val isChecked: Boolean = false,
    val isExpandable: Boolean = false,
    val fileCount: Int? = null,
) {
    /**
     * 获取所有已勾选的叶子节点（文件或不可展开的目录）。
     */
    val checkedLeafNodes: List<TreeFileNode>
        get() = buildList {
            if (isChecked && (!isDirectory || !isExpandable)) {
                add(this@TreeFileNode)
            }
            children.forEach { addAll(it.checkedLeafNodes) }
        }

    /**
     * 是否所有直接子节点都已勾选。
     */
    val areAllChildrenChecked: Boolean
        get() = children.isNotEmpty() && children.all { it.isChecked }

    /**
     * 直接子节点数量。
     */
    val directChildCount: Int
        get() = children.size
}
