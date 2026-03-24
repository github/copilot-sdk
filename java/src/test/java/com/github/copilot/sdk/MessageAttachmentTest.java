/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.copilot.sdk.json.Attachment;
import com.github.copilot.sdk.json.BlobAttachment;
import com.github.copilot.sdk.json.MessageAttachment;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SendMessageRequest;

/**
 * Tests for the {@link MessageAttachment} sealed interface and type-safe
 * attachment handling.
 */
class MessageAttachmentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =========================================================================
    // Sealed interface hierarchy
    // =========================================================================

    @Test
    void attachmentImplementsMessageAttachment() {
        Attachment attachment = new Attachment("file", "/path/to/file.java", "Source");
        assertInstanceOf(MessageAttachment.class, attachment);
        assertEquals("file", attachment.getType());
    }

    @Test
    void blobAttachmentImplementsMessageAttachment() {
        BlobAttachment blob = new BlobAttachment().setData("aGVsbG8=").setMimeType("image/png")
                .setDisplayName("test.png");
        assertInstanceOf(MessageAttachment.class, blob);
        assertEquals("blob", blob.getType());
    }

    // =========================================================================
    // MessageOptions type safety
    // =========================================================================

    @Test
    void setAttachmentsAcceptsListOfAttachment() {
        MessageOptions options = new MessageOptions();
        List<Attachment> list = List.of(new Attachment("file", "/a.java", "A"));
        options.setAttachments(list);

        assertEquals(1, options.getAttachments().size());
        assertInstanceOf(Attachment.class, options.getAttachments().get(0));
    }

    @Test
    void setAttachmentsAcceptsListOfBlobAttachment() {
        MessageOptions options = new MessageOptions();
        List<BlobAttachment> list = List.of(new BlobAttachment().setData("ZGF0YQ==").setMimeType("image/jpeg"));
        options.setAttachments(list);

        assertEquals(1, options.getAttachments().size());
        assertInstanceOf(BlobAttachment.class, options.getAttachments().get(0));
    }

    @Test
    void setAttachmentsAcceptsMixedList() {
        MessageOptions options = new MessageOptions();
        List<MessageAttachment> mixed = List.of(new Attachment("file", "/a.java", "A"),
                new BlobAttachment().setData("ZGF0YQ==").setMimeType("image/png"));
        options.setAttachments(mixed);

        assertEquals(2, options.getAttachments().size());
        assertInstanceOf(Attachment.class, options.getAttachments().get(0));
        assertInstanceOf(BlobAttachment.class, options.getAttachments().get(1));
    }

    @Test
    void setAttachmentsHandlesNull() {
        MessageOptions options = new MessageOptions();
        options.setAttachments(null);
        assertNull(options.getAttachments());
    }

    @Test
    void getAttachmentsReturnsUnmodifiableList() {
        MessageOptions options = new MessageOptions();
        options.setAttachments(List.of(new Attachment("file", "/a.java", "A")));
        assertThrows(UnsupportedOperationException.class,
                () -> options.getAttachments().add(new Attachment("file", "/b.java", "B")));
    }

    // =========================================================================
    // SendMessageRequest type safety
    // =========================================================================

    @Test
    void sendMessageRequestAcceptsMessageAttachmentList() {
        SendMessageRequest request = new SendMessageRequest();
        List<MessageAttachment> list = List.of(new Attachment("file", "/a.java", "A"),
                new BlobAttachment().setData("ZGF0YQ==").setMimeType("image/png"));
        request.setAttachments(list);

        assertEquals(2, request.getAttachments().size());
    }

    // =========================================================================
    // Jackson serialization
    // =========================================================================

    @Test
    void serializeAttachmentIncludesType() throws Exception {
        Attachment attachment = new Attachment("file", "/path/to/file.java", "Source");
        String json = MAPPER.writeValueAsString(attachment);
        assertTrue(json.contains("\"type\":\"file\""));
        assertTrue(json.contains("\"path\":\"/path/to/file.java\""));
    }

    @Test
    void serializeBlobAttachmentIncludesType() throws Exception {
        BlobAttachment blob = new BlobAttachment().setData("aGVsbG8=").setMimeType("image/png")
                .setDisplayName("test.png");
        String json = MAPPER.writeValueAsString(blob);
        assertTrue(json.contains("\"type\":\"blob\""));
        assertTrue(json.contains("\"data\":\"aGVsbG8=\""));
        assertTrue(json.contains("\"mimeType\":\"image/png\""));
    }

    @Test
    void serializeMessageOptionsWithMixedAttachments() throws Exception {
        MessageOptions options = new MessageOptions().setPrompt("Describe")
                .setAttachments(List.of(new Attachment("file", "/a.java", "A"),
                        new BlobAttachment().setData("ZGF0YQ==").setMimeType("image/png").setDisplayName("img.png")));

        String json = MAPPER.writeValueAsString(options);
        assertTrue(json.contains("\"type\":\"file\""));
        assertTrue(json.contains("\"type\":\"blob\""));
    }

    @Test
    void cloneMessageOptionsPreservesAttachments() {
        MessageOptions original = new MessageOptions().setPrompt("test")
                .setAttachments(List.of(new Attachment("file", "/a.java", "A")));

        MessageOptions cloned = original.clone();

        assertEquals(1, cloned.getAttachments().size());
        assertInstanceOf(Attachment.class, cloned.getAttachments().get(0));
        // Verify clone is independent
        assertNotSame(original.getAttachments(), cloned.getAttachments());
    }
}
