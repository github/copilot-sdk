/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if !NET5_0_OR_GREATER
namespace System.Runtime.CompilerServices;

// Polyfill so [ModuleInitializer] compiles on net472; recognized by the compiler.
[AttributeUsage(AttributeTargets.Method, Inherited = false)]
internal sealed class ModuleInitializerAttribute : Attribute
{
}
#endif
