/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * GitHub Copilot SDK for Java.
 */
module com.github.copilot.sdk.java {
    requires transitive com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires static com.github.spotbugs.annotations;
    requires static java.compiler;
    requires static java.net.http;
    requires java.logging;

    exports com.github.copilot.sdk;
    exports com.github.copilot.sdk.generated;
    exports com.github.copilot.sdk.generated.rpc;
    exports com.github.copilot.sdk.json;

    opens com.github.copilot.sdk to com.fasterxml.jackson.databind;
    opens com.github.copilot.sdk.generated to com.fasterxml.jackson.databind;
    opens com.github.copilot.sdk.json to com.fasterxml.jackson.databind;
}
