package dev.wucheng.resource_viewer.data.remote.smb

/**
 * SMB 相关异常类。
 * 用于在 SmbClientWrapper 中抛出，由 SmbFileSource 捕获并转换为 DomainError。
 */

/**
 * SMB 连接异常。
 * 当无法连接到 SMB 服务器时抛出。
 */
class SmbConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * SMB 认证异常。
 * 当 SMB 认证失败时抛出。
 */
class SmbAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * SMB 文件操作异常。
 * 当 SMB 文件操作失败时抛出。
 */
class SmbFileException(message: String, cause: Throwable? = null) : Exception(message, cause)
