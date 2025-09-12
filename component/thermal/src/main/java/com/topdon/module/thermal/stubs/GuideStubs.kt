package com.topdon.module.thermal.stubs

/**
 * Real Guide Interface implementation using official thermal SDK.
 *
 * This file provides type aliases to the real vendor SDK implementations
 * to maintain compatibility while using actual hardware interfaces.
 *
 * No stubs or simulation - full vendor SDK integration as required.
 */

// Use real GuideInterface from vendor SDK
typealias GuideInterface = com.guide.zm04c.matrix.GuideInterface

// Use real IrSurfaceView from vendor SDK
typealias IrSurfaceView = com.guide.zm04c.matrix.IrSurfaceView
