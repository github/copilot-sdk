/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

export const registerPrivateAppExtensionSymbol = Symbol("registerPrivateAppExtension");
export const registerPrivateAppSessionBadgesSymbol = Symbol("registerPrivateAppSessionBadges");
export const onExtensionTransportClosedSymbol = Symbol("onExtensionTransportClosed");
export const registerPrivateAppCanvasSymbol = Symbol("registerPrivateAppCanvas");
export const unregisterPrivateAppCanvasSymbol = Symbol("unregisterPrivateAppCanvas");
export const registerPrivateAppForgeProviderSymbol = Symbol("registerPrivateAppForgeProvider");
export const unregisterPrivateAppForgeProviderSymbol = Symbol("unregisterPrivateAppForgeProvider");
export const requestPrivateAppMediatedFetchSymbol = Symbol("requestPrivateAppMediatedFetch");
