"""
Unit tests for generated session event types.

Tests for parsing session events and their data structures from the generated
session_events.py module.
"""


from copilot.generated.session_events import (
    Attachment,
    AttachmentType,
    SessionEventType,
)


class TestSessionEventTypes:
    """Test session event type enum values."""

    def test_session_start_event_type(self):
        """The session.start event type should be recognized."""
        assert SessionEventType.SESSION_START.value == "session.start"

    def test_session_snapshot_rewind_event_type(self):
        """The session.snapshot_rewind event type should be recognized."""
        assert SessionEventType.SESSION_SNAPSHOT_REWIND.value == "session.snapshot_rewind"

    def test_session_usage_info_event_type(self):
        """The session.usage_info event type should be recognized."""
        assert SessionEventType.SESSION_USAGE_INFO.value == "session.usage_info"


class TestAttachmentTypes:
    """Test attachment type parsing from generated session_events module."""

    def test_file_attachment_parsing(self):
        """Test parsing a file attachment."""
        attachment_dict = {
            "type": "file",
            "path": "/path/to/file.py",
            "displayName": "file.py",
        }

        attachment = Attachment.from_dict(attachment_dict)
        assert attachment.type == AttachmentType.FILE
        assert attachment.path == "/path/to/file.py"
        assert attachment.display_name == "file.py"

    def test_directory_attachment_parsing(self):
        """Test parsing a directory attachment."""
        attachment_dict = {
            "type": "directory",
            "path": "/path/to/dir",
            "displayName": "dir",
        }

        attachment = Attachment.from_dict(attachment_dict)
        assert attachment.type == AttachmentType.DIRECTORY
        assert attachment.path == "/path/to/dir"
        assert attachment.display_name == "dir"

    def test_selection_attachment_parsing(self):
        """Test parsing a selection attachment with all fields."""
        attachment_dict = {
            "type": "selection",
            "filePath": "/path/to/file.py",
            "displayName": "file.py:10-20",
            "selection": {
                "start": {"line": 10, "character": 0},
                "end": {"line": 20, "character": 50},
            },
            "text": "selected text content",
        }

        attachment = Attachment.from_dict(attachment_dict)
        assert attachment.type == AttachmentType.SELECTION
        assert attachment.file_path == "/path/to/file.py"
        assert attachment.display_name == "file.py:10-20"
        assert attachment.selection is not None
        assert attachment.selection.start.line == 10
        assert attachment.selection.start.character == 0
        assert attachment.selection.end.line == 20
        assert attachment.selection.end.character == 50
        assert attachment.text == "selected text content"

    def test_selection_attachment_minimal(self):
        """Test parsing a selection attachment with only required fields."""
        attachment_dict = {
            "type": "selection",
            "filePath": "/path/to/file.py",
            "displayName": "file.py",
        }

        attachment = Attachment.from_dict(attachment_dict)
        assert attachment.type == AttachmentType.SELECTION
        assert attachment.file_path == "/path/to/file.py"
        assert attachment.selection is None
        assert attachment.text is None
