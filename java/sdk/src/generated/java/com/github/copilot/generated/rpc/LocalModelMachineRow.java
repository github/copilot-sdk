/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * One local model and its current files, progress, and requirements.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelMachineRow.$NullableDeserializer.class)
public record LocalModelMachineRow(
    /** Resolved catalog model. */
    @JsonProperty("descriptor") LocalModelDescriptor descriptor,
    /** Effective installation or download state. */
    @JsonProperty("status") LocalModelStatus status,
    /** Partial bytes in the selected version directory. */
    @JsonProperty("stagedBytes") Double stagedBytes,
    /** Bytes in inactive version directories. */
    @JsonProperty("leftoverBytes") Double leftoverBytes,
    /** Disk availability and required space. */
    @JsonProperty("disk") LocalModelDiskRequirement disk,
    /** Graphics memory availability and required space. */
    @JsonProperty("gpu") LocalModelGpuRequirement gpu,
    /** Remaining peak space with the existing safety allowance. */
    @JsonProperty("diskRequiredBytes") Double diskRequiredBytes,
    /** Whether known disk facts permit installation. */
    @JsonProperty("diskMet") Boolean diskMet,
    /** Published recommended graphics memory. */
    @JsonProperty("gpuRequiredBytes") Double gpuRequiredBytes,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelMachineRow { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelMachineRow(LocalModelDescriptor descriptor, LocalModelStatus status, Double stagedBytes, Double leftoverBytes, LocalModelDiskRequirement disk, LocalModelGpuRequirement gpu, Double diskRequiredBytes, Boolean diskMet, Double gpuRequiredBytes) {
        this(descriptor, status, stagedBytes, leftoverBytes, disk, gpu, diskRequiredBytes, diskMet, gpuRequiredBytes, java.util.Set.of());
    }

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private LocalModelDescriptor descriptor;
        private LocalModelStatus status;
        private Double stagedBytes;
        private Double leftoverBytes;
        private LocalModelDiskRequirement disk;
        private LocalModelGpuRequirement gpu;
        private Double diskRequiredBytes;
        private boolean $presence6;
        private Boolean diskMet;
        private Double gpuRequiredBytes;
        private boolean $presence8;

        /**
         * Sets the {@code descriptor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder descriptor(LocalModelDescriptor value) {
            this.descriptor = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(LocalModelStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code stagedBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder stagedBytes(Double value) {
            this.stagedBytes = value;
            return this;
        }

        /**
         * Sets the {@code leftoverBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder leftoverBytes(Double value) {
            this.leftoverBytes = value;
            return this;
        }

        /**
         * Sets the {@code disk} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disk(LocalModelDiskRequirement value) {
            this.disk = value;
            return this;
        }

        /**
         * Sets the {@code gpu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder gpu(LocalModelGpuRequirement value) {
            this.gpu = value;
            return this;
        }

        /**
         * Sets the {@code diskRequiredBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder diskRequiredBytes(Double value) {
            this.diskRequiredBytes = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code diskMet} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder diskMet(Boolean value) {
            this.diskMet = value;
            return this;
        }

        /**
         * Sets the {@code gpuRequiredBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder gpuRequiredBytes(Double value) {
            this.gpuRequiredBytes = value;
            this.$presence8 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelMachineRow build() {
            if (descriptor == null) throw new IllegalStateException("Missing required property: descriptor");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (stagedBytes == null) throw new IllegalStateException("Missing required property: stagedBytes");
            if (leftoverBytes == null) throw new IllegalStateException("Missing required property: leftoverBytes");
            if (disk == null) throw new IllegalStateException("Missing required property: disk");
            if (gpu == null) throw new IllegalStateException("Missing required property: gpu");
            if (diskMet == null) throw new IllegalStateException("Missing required property: diskMet");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence6 && diskRequiredBytes == null) $explicitNulls.add("diskRequiredBytes");
            if ($presence8 && gpuRequiredBytes == null) $explicitNulls.add("gpuRequiredBytes");
            return new LocalModelMachineRow(descriptor, status, stagedBytes, leftoverBytes, disk, gpu, diskRequiredBytes, diskMet, gpuRequiredBytes, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("diskRequiredBytes") && diskRequiredBytes == null) values.put("diskRequiredBytes", null);
        if ($explicitNulls.contains("gpuRequiredBytes") && gpuRequiredBytes == null) values.put("gpuRequiredBytes", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelMachineRow> {
        @Override
        public LocalModelMachineRow deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelMachineRow.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("diskRequiredBytes") && tree.get("diskRequiredBytes").isNull()) $explicitNulls.add("diskRequiredBytes");
            if (tree.has("gpuRequiredBytes") && tree.get("gpuRequiredBytes").isNull()) $explicitNulls.add("gpuRequiredBytes");
            return new LocalModelMachineRow(readProperty(context, tree, "descriptor", new com.fasterxml.jackson.core.type.TypeReference<LocalModelDescriptor>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<LocalModelStatus>() {}), readProperty(context, tree, "stagedBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "leftoverBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "disk", new com.fasterxml.jackson.core.type.TypeReference<LocalModelDiskRequirement>() {}), readProperty(context, tree, "gpu", new com.fasterxml.jackson.core.type.TypeReference<LocalModelGpuRequirement>() {}), readProperty(context, tree, "diskRequiredBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "diskMet", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "gpuRequiredBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
