package com.appblish.filora.core.data.thumbnail

/**
 * Pure power-of-two downsample math used to bound the cost of decoding a full-size
 * image down to a grid-cell thumbnail (R6 "downsampled decode"). Computed before
 * the second decode pass so we never allocate the full bitmap in memory.
 */
object ThumbnailSampling {
    /**
     * Largest power-of-two `inSampleSize` that keeps the decoded bitmap at least as
     * large as [reqWidth] x [reqHeight]. Returns 1 (no downsample) when any
     * dimension is non-positive or the source already fits the request.
     */
    fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        if (intArrayOf(srcWidth, srcHeight, reqWidth, reqHeight).any { it <= 0 }) return 1

        var inSampleSize = 1
        val halfWidth = srcWidth / 2
        val halfHeight = srcHeight / 2
        while (halfWidth / inSampleSize >= reqWidth && halfHeight / inSampleSize >= reqHeight) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
