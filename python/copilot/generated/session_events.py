"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: session-events.schema.json
"""

from enum import Enum
from dataclasses import dataclass
from typing import Any, TypeVar, cast
from collections.abc import Callable
from datetime import datetime
from uuid import UUID
import dateutil.parser


T = TypeVar("T")
EnumT = TypeVar("EnumT", bound=Enum)


def from_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)


def to_float(x: Any) -> float:
    assert isinstance(x, (int, float))
    return x


def to_class(c: type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()


def from_str(x: Any) -> str:
    assert isinstance(x, str)
    return x


def from_none(x: Any) -> Any:
    assert x is None
    return x


def from_union(fs, x):
    for f in fs:
        try:
            return f(x)
        except Exception:
            pass
    assert False


def to_enum(c: type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, c)
    return x.value


def from_list(f: Callable[[Any], T], x: Any) -> list[T]:
    assert isinstance(x, list)
    return [f(y) for y in x]


def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:
    assert isinstance(x, dict)
    return { k: f(v) for (k, v) in x.items() }


def from_bool(x: Any) -> bool:
    assert isinstance(x, bool)
    return x


def from_datetime(x: Any) -> datetime:
    return dateutil.parser.parse(x)


def from_int(x: Any) -> int:
    assert isinstance(x, int) and not isinstance(x, bool)
    return x


class AgentMode(Enum):
    """The agent mode that was active when this message was sent"""

    AUTOPILOT = "autopilot"
    INTERACTIVE = "interactive"
    PLAN = "plan"
    SHELL = "shell"


@dataclass
class LineRange:
    """Optional line range to scope the attachment to a specific section of the file"""

    end: float
    """End line number (1-based, inclusive)"""

    start: float
    """Start line number (1-based)"""

    @staticmethod
    def from_dict(obj: Any) -> 'LineRange':
        assert isinstance(obj, dict)
        end = from_float(obj.get("end"))
        start = from_float(obj.get("start"))
        return LineRange(end, start)

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_float(self.end)
        result["start"] = to_float(self.start)
        return result


class ReferenceType(Enum):
    """Type of GitHub reference"""

    DISCUSSION = "discussion"
    ISSUE = "issue"
    PR = "pr"


@dataclass
class End:
    """End position of the selection"""

    character: float
    """End character offset within the line (0-based)"""

    line: float
    """End line number (0-based)"""

    @staticmethod
    def from_dict(obj: Any) -> 'End':
        assert isinstance(obj, dict)
        character = from_float(obj.get("character"))
        line = from_float(obj.get("line"))
        return End(character, line)

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = to_float(self.character)
        result["line"] = to_float(self.line)
        return result


@dataclass
class Start:
    """Start position of the selection"""

    character: float
    """Start character offset within the line (0-based)"""

    line: float
    """Start line number (0-based)"""

    @staticmethod
    def from_dict(obj: Any) -> 'Start':
        assert isinstance(obj, dict)
        character = from_float(obj.get("character"))
        line = from_float(obj.get("line"))
        return Start(character, line)

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = to_float(self.character)
        result["line"] = to_float(self.line)
        return result


@dataclass
class Selection:
    """Position range of the selection within the file"""

    end: End
    """End position of the selection"""

    start: Start
    """Start position of the selection"""

    @staticmethod
    def from_dict(obj: Any) -> 'Selection':
        assert isinstance(obj, dict)
        end = End.from_dict(obj.get("end"))
        start = Start.from_dict(obj.get("start"))
        return Selection(end, start)

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_class(End, self.end)
        result["start"] = to_class(Start, self.start)
        return result


class AttachmentType(Enum):
    BLOB = "blob"
    DIRECTORY = "directory"
    FILE = "file"
    GITHUB_REFERENCE = "github_reference"
    SELECTION = "selection"


@dataclass
class Attachment:
    """A user message attachment — a file, directory, code selection, blob, or GitHub reference
    
    File attachment
    
    Directory attachment
    
    Code selection attachment from an editor
    
    GitHub issue, pull request, or discussion reference
    
    Blob attachment with inline base64-encoded data
    """
    type: AttachmentType
    """Attachment type discriminator"""

    display_name: str | None = None
    """User-facing display name for the attachment
    
    User-facing display name for the selection
    """
    line_range: LineRange | None = None
    """Optional line range to scope the attachment to a specific section of the file"""

    path: str | None = None
    """Absolute file path
    
    Absolute directory path
    """
    file_path: str | None = None
    """Absolute path to the file containing the selection"""

    selection: Selection | None = None
    """Position range of the selection within the file"""

    text: str | None = None
    """The selected text content"""

    number: float | None = None
    """Issue, pull request, or discussion number"""

    reference_type: ReferenceType | None = None
    """Type of GitHub reference"""

    state: str | None = None
    """Current state of the referenced item (e.g., open, closed, merged)"""

    title: str | None = None
    """Title of the referenced item"""

    url: str | None = None
    """URL to the referenced item on GitHub"""

    data: str | None = None
    """Base64-encoded content"""

    mime_type: str | None = None
    """MIME type of the inline data"""

    @staticmethod
    def from_dict(obj: Any) -> 'Attachment':
        assert isinstance(obj, dict)
        type = AttachmentType(obj.get("type"))
        display_name = from_union([from_str, from_none], obj.get("displayName"))
        line_range = from_union([LineRange.from_dict, from_none], obj.get("lineRange"))
        path = from_union([from_str, from_none], obj.get("path"))
        file_path = from_union([from_str, from_none], obj.get("filePath"))
        selection = from_union([Selection.from_dict, from_none], obj.get("selection"))
        text = from_union([from_str, from_none], obj.get("text"))
        number = from_union([from_float, from_none], obj.get("number"))
        reference_type = from_union([ReferenceType, from_none], obj.get("referenceType"))
        state = from_union([from_str, from_none], obj.get("state"))
        title = from_union([from_str, from_none], obj.get("title"))
        url = from_union([from_str, from_none], obj.get("url"))
        data = from_union([from_str, from_none], obj.get("data"))
        mime_type = from_union([from_str, from_none], obj.get("mimeType"))
        return Attachment(type, display_name, line_range, path, file_path, selection, text, number, reference_type, state, title, url, data, mime_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(AttachmentType, self.type)
        if self.display_name is not None:
            result["displayName"] = from_union([from_str, from_none], self.display_name)
        if self.line_range is not None:
            result["lineRange"] = from_union([lambda x: to_class(LineRange, x), from_none], self.line_range)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.file_path is not None:
            result["filePath"] = from_union([from_str, from_none], self.file_path)
        if self.selection is not None:
            result["selection"] = from_union([lambda x: to_class(Selection, x), from_none], self.selection)
        if self.text is not None:
            result["text"] = from_union([from_str, from_none], self.text)
        if self.number is not None:
            result["number"] = from_union([to_float, from_none], self.number)
        if self.reference_type is not None:
            result["referenceType"] = from_union([lambda x: to_enum(ReferenceType, x), from_none], self.reference_type)
        if self.state is not None:
            result["state"] = from_union([from_str, from_none], self.state)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        if self.data is not None:
            result["data"] = from_union([from_str, from_none], self.data)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_str, from_none], self.mime_type)
        return result


@dataclass
class Agent:
    """A background agent task"""

    agent_id: str
    """Unique identifier of the background agent"""

    agent_type: str
    """Type of the background agent"""

    description: str | None = None
    """Human-readable description of the agent task"""

    @staticmethod
    def from_dict(obj: Any) -> 'Agent':
        assert isinstance(obj, dict)
        agent_id = from_str(obj.get("agentId"))
        agent_type = from_str(obj.get("agentType"))
        description = from_union([from_str, from_none], obj.get("description"))
        return Agent(agent_id, agent_type, description)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentId"] = from_str(self.agent_id)
        result["agentType"] = from_str(self.agent_type)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        return result


@dataclass
class Shell:
    """A background shell command"""

    shell_id: str
    """Unique identifier of the background shell"""

    description: str | None = None
    """Human-readable description of the shell command"""

    @staticmethod
    def from_dict(obj: Any) -> 'Shell':
        assert isinstance(obj, dict)
        shell_id = from_str(obj.get("shellId"))
        description = from_union([from_str, from_none], obj.get("description"))
        return Shell(shell_id, description)

    def to_dict(self) -> dict:
        result: dict = {}
        result["shellId"] = from_str(self.shell_id)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        return result


@dataclass
class BackgroundTasks:
    """Background tasks still running when the agent became idle"""

    agents: list[Agent]
    """Currently running background agents"""

    shells: list[Shell]
    """Currently running background shell commands"""

    @staticmethod
    def from_dict(obj: Any) -> 'BackgroundTasks':
        assert isinstance(obj, dict)
        agents = from_list(Agent.from_dict, obj.get("agents"))
        shells = from_list(Shell.from_dict, obj.get("shells"))
        return BackgroundTasks(agents, shells)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(Agent, x), self.agents)
        result["shells"] = from_list(lambda x: to_class(Shell, x), self.shells)
        return result


@dataclass
class CodeChanges:
    """Aggregate code change metrics for the session"""

    files_modified: list[str]
    """List of file paths that were modified during the session"""

    lines_added: float
    """Total number of lines added during the session"""

    lines_removed: float
    """Total number of lines removed during the session"""

    @staticmethod
    def from_dict(obj: Any) -> 'CodeChanges':
        assert isinstance(obj, dict)
        files_modified = from_list(from_str, obj.get("filesModified"))
        lines_added = from_float(obj.get("linesAdded"))
        lines_removed = from_float(obj.get("linesRemoved"))
        return CodeChanges(files_modified, lines_added, lines_removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["filesModified"] = from_list(from_str, self.files_modified)
        result["linesAdded"] = to_float(self.lines_added)
        result["linesRemoved"] = to_float(self.lines_removed)
        return result


@dataclass
class CompactionTokensUsed:
    """Token usage breakdown for the compaction LLM call"""

    cached_input: float
    """Cached input tokens reused in the compaction LLM call"""

    input: float
    """Input tokens consumed by the compaction LLM call"""

    output: float
    """Output tokens produced by the compaction LLM call"""

    @staticmethod
    def from_dict(obj: Any) -> 'CompactionTokensUsed':
        assert isinstance(obj, dict)
        cached_input = from_float(obj.get("cachedInput"))
        input = from_float(obj.get("input"))
        output = from_float(obj.get("output"))
        return CompactionTokensUsed(cached_input, input, output)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cachedInput"] = to_float(self.cached_input)
        result["input"] = to_float(self.input)
        result["output"] = to_float(self.output)
        return result


class HostType(Enum):
    """Hosting platform type of the repository (github or ado)"""

    ADO = "ado"
    GITHUB = "github"


@dataclass
class ContextClass:
    """Working directory and git context at session start
    
    Updated working directory and git context at resume time
    """
    cwd: str
    """Current working directory path"""

    base_commit: str | None = None
    """Base commit of current git branch at session start time"""

    branch: str | None = None
    """Current git branch name"""

    git_root: str | None = None
    """Root directory of the git repository, resolved via git rev-parse"""

    head_commit: str | None = None
    """Head commit of current git branch at session start time"""

    host_type: HostType | None = None
    """Hosting platform type of the repository (github or ado)"""

    repository: str | None = None
    """Repository identifier derived from the git remote URL ("owner/name" for GitHub,
    "org/project/repo" for Azure DevOps)
    """

    @staticmethod
    def from_dict(obj: Any) -> 'ContextClass':
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        base_commit = from_union([from_str, from_none], obj.get("baseCommit"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        git_root = from_union([from_str, from_none], obj.get("gitRoot"))
        head_commit = from_union([from_str, from_none], obj.get("headCommit"))
        host_type = from_union([HostType, from_none], obj.get("hostType"))
        repository = from_union([from_str, from_none], obj.get("repository"))
        return ContextClass(cwd, base_commit, branch, git_root, head_commit, host_type, repository)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_str, from_none], self.base_commit)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_str, from_none], self.git_root)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_str, from_none], self.head_commit)
        if self.host_type is not None:
            result["hostType"] = from_union([lambda x: to_enum(HostType, x), from_none], self.host_type)
        if self.repository is not None:
            result["repository"] = from_union([from_str, from_none], self.repository)
        return result


@dataclass
class TokenDetail:
    """Token usage detail for a single billing category"""

    batch_size: float
    """Number of tokens in this billing batch"""

    cost_per_batch: float
    """Cost per batch of tokens"""

    token_count: float
    """Total token count for this entry"""

    token_type: str
    """Token category (e.g., "input", "output")"""

    @staticmethod
    def from_dict(obj: Any) -> 'TokenDetail':
        assert isinstance(obj, dict)
        batch_size = from_float(obj.get("batchSize"))
        cost_per_batch = from_float(obj.get("costPerBatch"))
        token_count = from_float(obj.get("tokenCount"))
        token_type = from_str(obj.get("tokenType"))
        return TokenDetail(batch_size, cost_per_batch, token_count, token_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["batchSize"] = to_float(self.batch_size)
        result["costPerBatch"] = to_float(self.cost_per_batch)
        result["tokenCount"] = to_float(self.token_count)
        result["tokenType"] = from_str(self.token_type)
        return result


@dataclass
class CopilotUsage:
    """Per-request cost and usage data from the CAPI copilot_usage response field"""

    token_details: list[TokenDetail]
    """Itemized token usage breakdown"""

    total_nano_aiu: float
    """Total cost in nano-AIU (AI Units) for this request"""

    @staticmethod
    def from_dict(obj: Any) -> 'CopilotUsage':
        assert isinstance(obj, dict)
        token_details = from_list(TokenDetail.from_dict, obj.get("tokenDetails"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        return CopilotUsage(token_details, total_nano_aiu)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenDetails"] = from_list(lambda x: to_class(TokenDetail, x), self.token_details)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        return result


@dataclass
class ErrorClass:
    """Error details when the tool execution failed
    
    Error details when the hook failed
    """
    message: str
    """Human-readable error message"""

    code: str | None = None
    """Machine-readable error code"""

    stack: str | None = None
    """Error stack trace, when available"""

    @staticmethod
    def from_dict(obj: Any) -> 'ErrorClass':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        code = from_union([from_str, from_none], obj.get("code"))
        stack = from_union([from_str, from_none], obj.get("stack"))
        return ErrorClass(message, code, stack)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.code is not None:
            result["code"] = from_union([from_str, from_none], self.code)
        if self.stack is not None:
            result["stack"] = from_union([from_str, from_none], self.stack)
        return result


class Status(Enum):
    """Whether the agent completed successfully or failed"""

    COMPLETED = "completed"
    FAILED = "failed"


class KindType(Enum):
    AGENT_COMPLETED = "agent_completed"
    SHELL_COMPLETED = "shell_completed"
    SHELL_DETACHED_COMPLETED = "shell_detached_completed"


@dataclass
class KindClass:
    """Structured metadata identifying what triggered this notification"""

    type: KindType
    agent_id: str | None = None
    """Unique identifier of the background agent"""

    agent_type: str | None = None
    """Type of the agent (e.g., explore, task, general-purpose)"""

    description: str | None = None
    """Human-readable description of the agent task
    
    Human-readable description of the command
    """
    prompt: str | None = None
    """The full prompt given to the background agent"""

    status: Status | None = None
    """Whether the agent completed successfully or failed"""

    exit_code: float | None = None
    """Exit code of the shell command, if available"""

    shell_id: str | None = None
    """Unique identifier of the shell session
    
    Unique identifier of the detached shell session
    """

    @staticmethod
    def from_dict(obj: Any) -> 'KindClass':
        assert isinstance(obj, dict)
        type = KindType(obj.get("type"))
        agent_id = from_union([from_str, from_none], obj.get("agentId"))
        agent_type = from_union([from_str, from_none], obj.get("agentType"))
        description = from_union([from_str, from_none], obj.get("description"))
        prompt = from_union([from_str, from_none], obj.get("prompt"))
        status = from_union([Status, from_none], obj.get("status"))
        exit_code = from_union([from_float, from_none], obj.get("exitCode"))
        shell_id = from_union([from_str, from_none], obj.get("shellId"))
        return KindClass(type, agent_id, agent_type, description, prompt, status, exit_code, shell_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(KindType, self.type)
        if self.agent_id is not None:
            result["agentId"] = from_union([from_str, from_none], self.agent_id)
        if self.agent_type is not None:
            result["agentType"] = from_union([from_str, from_none], self.agent_type)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.prompt is not None:
            result["prompt"] = from_union([from_str, from_none], self.prompt)
        if self.status is not None:
            result["status"] = from_union([lambda x: to_enum(Status, x), from_none], self.status)
        if self.exit_code is not None:
            result["exitCode"] = from_union([to_float, from_none], self.exit_code)
        if self.shell_id is not None:
            result["shellId"] = from_union([from_str, from_none], self.shell_id)
        return result


@dataclass
class Metadata:
    """Metadata about the prompt template and its construction"""

    prompt_version: str | None = None
    """Version identifier of the prompt template used"""

    variables: dict[str, Any] | None = None
    """Template variables used when constructing the prompt"""

    @staticmethod
    def from_dict(obj: Any) -> 'Metadata':
        assert isinstance(obj, dict)
        prompt_version = from_union([from_str, from_none], obj.get("promptVersion"))
        variables = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("variables"))
        return Metadata(prompt_version, variables)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.prompt_version is not None:
            result["promptVersion"] = from_union([from_str, from_none], self.prompt_version)
        if self.variables is not None:
            result["variables"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.variables)
        return result


class Mode(Enum):
    FORM = "form"


@dataclass
class Requests:
    """Request count and cost metrics"""

    cost: float
    """Cumulative cost multiplier for requests to this model"""

    count: float
    """Total number of API requests made to this model"""

    @staticmethod
    def from_dict(obj: Any) -> 'Requests':
        assert isinstance(obj, dict)
        cost = from_float(obj.get("cost"))
        count = from_float(obj.get("count"))
        return Requests(cost, count)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cost"] = to_float(self.cost)
        result["count"] = to_float(self.count)
        return result


@dataclass
class Usage:
    """Token usage breakdown"""

    cache_read_tokens: float
    """Total tokens read from prompt cache across all requests"""

    cache_write_tokens: float
    """Total tokens written to prompt cache across all requests"""

    input_tokens: float
    """Total input tokens consumed across all requests to this model"""

    output_tokens: float
    """Total output tokens produced across all requests to this model"""

    @staticmethod
    def from_dict(obj: Any) -> 'Usage':
        assert isinstance(obj, dict)
        cache_read_tokens = from_float(obj.get("cacheReadTokens"))
        cache_write_tokens = from_float(obj.get("cacheWriteTokens"))
        input_tokens = from_float(obj.get("inputTokens"))
        output_tokens = from_float(obj.get("outputTokens"))
        return Usage(cache_read_tokens, cache_write_tokens, input_tokens, output_tokens)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cacheReadTokens"] = to_float(self.cache_read_tokens)
        result["cacheWriteTokens"] = to_float(self.cache_write_tokens)
        result["inputTokens"] = to_float(self.input_tokens)
        result["outputTokens"] = to_float(self.output_tokens)
        return result


@dataclass
class ModelMetric:
    requests: Requests
    """Request count and cost metrics"""

    usage: Usage
    """Token usage breakdown"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelMetric':
        assert isinstance(obj, dict)
        requests = Requests.from_dict(obj.get("requests"))
        usage = Usage.from_dict(obj.get("usage"))
        return ModelMetric(requests, usage)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requests"] = to_class(Requests, self.requests)
        result["usage"] = to_class(Usage, self.usage)
        return result


class Operation(Enum):
    """The type of operation performed on the plan file
    
    Whether the file was newly created or updated
    """
    CREATE = "create"
    DELETE = "delete"
    UPDATE = "update"


@dataclass
class Command:
    identifier: str
    """Command identifier (e.g., executable name)"""

    read_only: bool
    """Whether this command is read-only (no side effects)"""

    @staticmethod
    def from_dict(obj: Any) -> 'Command':
        assert isinstance(obj, dict)
        identifier = from_str(obj.get("identifier"))
        read_only = from_bool(obj.get("readOnly"))
        return Command(identifier, read_only)

    def to_dict(self) -> dict:
        result: dict = {}
        result["identifier"] = from_str(self.identifier)
        result["readOnly"] = from_bool(self.read_only)
        return result


class PermissionRequestKind(Enum):
    CUSTOM_TOOL = "custom-tool"
    HOOK = "hook"
    MCP = "mcp"
    MEMORY = "memory"
    READ = "read"
    SHELL = "shell"
    URL = "url"
    WRITE = "write"


@dataclass
class PossibleURL:
    url: str
    """URL that may be accessed by the command"""

    @staticmethod
    def from_dict(obj: Any) -> 'PossibleURL':
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        return PossibleURL(url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["url"] = from_str(self.url)
        return result


@dataclass
class PermissionRequest:
    """Details of the permission being requested
    
    Shell command permission request
    
    File write permission request
    
    File or directory read permission request
    
    MCP tool invocation permission request
    
    URL access permission request
    
    Memory storage permission request
    
    Custom tool invocation permission request
    
    Hook confirmation permission request
    """
    kind: PermissionRequestKind
    """Permission kind discriminator"""

    can_offer_session_approval: bool | None = None
    """Whether the UI can offer session-wide approval for this command pattern"""

    commands: list[Command] | None = None
    """Parsed command identifiers found in the command text"""

    full_command_text: str | None = None
    """The complete shell command text to be executed"""

    has_write_file_redirection: bool | None = None
    """Whether the command includes a file write redirection (e.g., > or >>)"""

    intention: str | None = None
    """Human-readable description of what the command intends to do
    
    Human-readable description of the intended file change
    
    Human-readable description of why the file is being read
    
    Human-readable description of why the URL is being accessed
    """
    possible_paths: list[str] | None = None
    """File paths that may be read or written by the command"""

    possible_urls: list[PossibleURL] | None = None
    """URLs that may be accessed by the command"""

    tool_call_id: str | None = None
    """Tool call ID that triggered this permission request"""

    warning: str | None = None
    """Optional warning message about risks of running this command"""

    diff: str | None = None
    """Unified diff showing the proposed changes"""

    file_name: str | None = None
    """Path of the file being written to"""

    new_file_contents: str | None = None
    """Complete new file contents for newly created files"""

    path: str | None = None
    """Path of the file or directory being read"""

    args: Any = None
    """Arguments to pass to the MCP tool
    
    Arguments to pass to the custom tool
    """
    read_only: bool | None = None
    """Whether this MCP tool is read-only (no side effects)"""

    server_name: str | None = None
    """Name of the MCP server providing the tool"""

    tool_name: str | None = None
    """Internal name of the MCP tool
    
    Name of the custom tool
    
    Name of the tool the hook is gating
    """
    tool_title: str | None = None
    """Human-readable title of the MCP tool"""

    url: str | None = None
    """URL to be fetched"""

    citations: str | None = None
    """Source references for the stored fact"""

    fact: str | None = None
    """The fact or convention being stored"""

    subject: str | None = None
    """Topic or subject of the memory being stored"""

    tool_description: str | None = None
    """Description of what the custom tool does"""

    hook_message: str | None = None
    """Optional message from the hook explaining why confirmation is needed"""

    tool_args: Any = None
    """Arguments of the tool call being gated"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionRequest':
        assert isinstance(obj, dict)
        kind = PermissionRequestKind(obj.get("kind"))
        can_offer_session_approval = from_union([from_bool, from_none], obj.get("canOfferSessionApproval"))
        commands = from_union([lambda x: from_list(Command.from_dict, x), from_none], obj.get("commands"))
        full_command_text = from_union([from_str, from_none], obj.get("fullCommandText"))
        has_write_file_redirection = from_union([from_bool, from_none], obj.get("hasWriteFileRedirection"))
        intention = from_union([from_str, from_none], obj.get("intention"))
        possible_paths = from_union([lambda x: from_list(from_str, x), from_none], obj.get("possiblePaths"))
        possible_urls = from_union([lambda x: from_list(PossibleURL.from_dict, x), from_none], obj.get("possibleUrls"))
        tool_call_id = from_union([from_str, from_none], obj.get("toolCallId"))
        warning = from_union([from_str, from_none], obj.get("warning"))
        diff = from_union([from_str, from_none], obj.get("diff"))
        file_name = from_union([from_str, from_none], obj.get("fileName"))
        new_file_contents = from_union([from_str, from_none], obj.get("newFileContents"))
        path = from_union([from_str, from_none], obj.get("path"))
        args = obj.get("args")
        read_only = from_union([from_bool, from_none], obj.get("readOnly"))
        server_name = from_union([from_str, from_none], obj.get("serverName"))
        tool_name = from_union([from_str, from_none], obj.get("toolName"))
        tool_title = from_union([from_str, from_none], obj.get("toolTitle"))
        url = from_union([from_str, from_none], obj.get("url"))
        citations = from_union([from_str, from_none], obj.get("citations"))
        fact = from_union([from_str, from_none], obj.get("fact"))
        subject = from_union([from_str, from_none], obj.get("subject"))
        tool_description = from_union([from_str, from_none], obj.get("toolDescription"))
        hook_message = from_union([from_str, from_none], obj.get("hookMessage"))
        tool_args = obj.get("toolArgs")
        return PermissionRequest(kind, can_offer_session_approval, commands, full_command_text, has_write_file_redirection, intention, possible_paths, possible_urls, tool_call_id, warning, diff, file_name, new_file_contents, path, args, read_only, server_name, tool_name, tool_title, url, citations, fact, subject, tool_description, hook_message, tool_args)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionRequestKind, self.kind)
        if self.can_offer_session_approval is not None:
            result["canOfferSessionApproval"] = from_union([from_bool, from_none], self.can_offer_session_approval)
        if self.commands is not None:
            result["commands"] = from_union([lambda x: from_list(lambda x: to_class(Command, x), x), from_none], self.commands)
        if self.full_command_text is not None:
            result["fullCommandText"] = from_union([from_str, from_none], self.full_command_text)
        if self.has_write_file_redirection is not None:
            result["hasWriteFileRedirection"] = from_union([from_bool, from_none], self.has_write_file_redirection)
        if self.intention is not None:
            result["intention"] = from_union([from_str, from_none], self.intention)
        if self.possible_paths is not None:
            result["possiblePaths"] = from_union([lambda x: from_list(from_str, x), from_none], self.possible_paths)
        if self.possible_urls is not None:
            result["possibleUrls"] = from_union([lambda x: from_list(lambda x: to_class(PossibleURL, x), x), from_none], self.possible_urls)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_str, from_none], self.tool_call_id)
        if self.warning is not None:
            result["warning"] = from_union([from_str, from_none], self.warning)
        if self.diff is not None:
            result["diff"] = from_union([from_str, from_none], self.diff)
        if self.file_name is not None:
            result["fileName"] = from_union([from_str, from_none], self.file_name)
        if self.new_file_contents is not None:
            result["newFileContents"] = from_union([from_str, from_none], self.new_file_contents)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.args is not None:
            result["args"] = self.args
        if self.read_only is not None:
            result["readOnly"] = from_union([from_bool, from_none], self.read_only)
        if self.server_name is not None:
            result["serverName"] = from_union([from_str, from_none], self.server_name)
        if self.tool_name is not None:
            result["toolName"] = from_union([from_str, from_none], self.tool_name)
        if self.tool_title is not None:
            result["toolTitle"] = from_union([from_str, from_none], self.tool_title)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        if self.citations is not None:
            result["citations"] = from_union([from_str, from_none], self.citations)
        if self.fact is not None:
            result["fact"] = from_union([from_str, from_none], self.fact)
        if self.subject is not None:
            result["subject"] = from_union([from_str, from_none], self.subject)
        if self.tool_description is not None:
            result["toolDescription"] = from_union([from_str, from_none], self.tool_description)
        if self.hook_message is not None:
            result["hookMessage"] = from_union([from_str, from_none], self.hook_message)
        if self.tool_args is not None:
            result["toolArgs"] = self.tool_args
        return result


@dataclass
class QuotaSnapshot:
    entitlement_requests: float
    """Total requests allowed by the entitlement"""

    is_unlimited_entitlement: bool
    """Whether the user has an unlimited usage entitlement"""

    overage: float
    """Number of requests over the entitlement limit"""

    overage_allowed_with_exhausted_quota: bool
    """Whether overage is allowed when quota is exhausted"""

    remaining_percentage: float
    """Percentage of quota remaining (0.0 to 1.0)"""

    usage_allowed_with_exhausted_quota: bool
    """Whether usage is still permitted after quota exhaustion"""

    used_requests: float
    """Number of requests already consumed"""

    reset_date: datetime | None = None
    """Date when the quota resets"""

    @staticmethod
    def from_dict(obj: Any) -> 'QuotaSnapshot':
        assert isinstance(obj, dict)
        entitlement_requests = from_float(obj.get("entitlementRequests"))
        is_unlimited_entitlement = from_bool(obj.get("isUnlimitedEntitlement"))
        overage = from_float(obj.get("overage"))
        overage_allowed_with_exhausted_quota = from_bool(obj.get("overageAllowedWithExhaustedQuota"))
        remaining_percentage = from_float(obj.get("remainingPercentage"))
        usage_allowed_with_exhausted_quota = from_bool(obj.get("usageAllowedWithExhaustedQuota"))
        used_requests = from_float(obj.get("usedRequests"))
        reset_date = from_union([from_datetime, from_none], obj.get("resetDate"))
        return QuotaSnapshot(entitlement_requests, is_unlimited_entitlement, overage, overage_allowed_with_exhausted_quota, remaining_percentage, usage_allowed_with_exhausted_quota, used_requests, reset_date)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entitlementRequests"] = to_float(self.entitlement_requests)
        result["isUnlimitedEntitlement"] = from_bool(self.is_unlimited_entitlement)
        result["overage"] = to_float(self.overage)
        result["overageAllowedWithExhaustedQuota"] = from_bool(self.overage_allowed_with_exhausted_quota)
        result["remainingPercentage"] = to_float(self.remaining_percentage)
        result["usageAllowedWithExhaustedQuota"] = from_bool(self.usage_allowed_with_exhausted_quota)
        result["usedRequests"] = to_float(self.used_requests)
        if self.reset_date is not None:
            result["resetDate"] = from_union([lambda x: x.isoformat(), from_none], self.reset_date)
        return result


@dataclass
class RepositoryClass:
    """Repository context for the handed-off session"""

    name: str
    """Repository name"""

    owner: str
    """Repository owner (user or organization)"""

    branch: str | None = None
    """Git branch name, if applicable"""

    @staticmethod
    def from_dict(obj: Any) -> 'RepositoryClass':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        owner = from_str(obj.get("owner"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        return RepositoryClass(name, owner, branch)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["owner"] = from_str(self.owner)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        return result


class RequestedSchemaType(Enum):
    OBJECT = "object"


@dataclass
class RequestedSchema:
    """JSON Schema describing the form fields to present to the user"""

    properties: dict[str, Any]
    """Form field definitions, keyed by field name"""

    type: RequestedSchemaType
    """Schema type indicator (always 'object')"""

    required: list[str] | None = None
    """List of required field names"""

    @staticmethod
    def from_dict(obj: Any) -> 'RequestedSchema':
        assert isinstance(obj, dict)
        properties = from_dict(lambda x: x, obj.get("properties"))
        type = RequestedSchemaType(obj.get("type"))
        required = from_union([lambda x: from_list(from_str, x), from_none], obj.get("required"))
        return RequestedSchema(properties, type, required)

    def to_dict(self) -> dict:
        result: dict = {}
        result["properties"] = from_dict(lambda x: x, self.properties)
        result["type"] = to_enum(RequestedSchemaType, self.type)
        if self.required is not None:
            result["required"] = from_union([lambda x: from_list(from_str, x), from_none], self.required)
        return result


class Theme(Enum):
    """Theme variant this icon is intended for"""

    DARK = "dark"
    LIGHT = "light"


@dataclass
class Icon:
    """Icon image for a resource"""

    src: str
    """URL or path to the icon image"""

    mime_type: str | None = None
    """MIME type of the icon image"""

    sizes: list[str] | None = None
    """Available icon sizes (e.g., ['16x16', '32x32'])"""

    theme: Theme | None = None
    """Theme variant this icon is intended for"""

    @staticmethod
    def from_dict(obj: Any) -> 'Icon':
        assert isinstance(obj, dict)
        src = from_str(obj.get("src"))
        mime_type = from_union([from_str, from_none], obj.get("mimeType"))
        sizes = from_union([lambda x: from_list(from_str, x), from_none], obj.get("sizes"))
        theme = from_union([Theme, from_none], obj.get("theme"))
        return Icon(src, mime_type, sizes, theme)

    def to_dict(self) -> dict:
        result: dict = {}
        result["src"] = from_str(self.src)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_str, from_none], self.mime_type)
        if self.sizes is not None:
            result["sizes"] = from_union([lambda x: from_list(from_str, x), from_none], self.sizes)
        if self.theme is not None:
            result["theme"] = from_union([lambda x: to_enum(Theme, x), from_none], self.theme)
        return result


@dataclass
class Resource:
    """The embedded resource contents, either text or base64-encoded binary"""

    uri: str
    """URI identifying the resource"""

    mime_type: str | None = None
    """MIME type of the text content
    
    MIME type of the blob content
    """
    text: str | None = None
    """Text content of the resource"""

    blob: str | None = None
    """Base64-encoded binary content of the resource"""

    @staticmethod
    def from_dict(obj: Any) -> 'Resource':
        assert isinstance(obj, dict)
        uri = from_str(obj.get("uri"))
        mime_type = from_union([from_str, from_none], obj.get("mimeType"))
        text = from_union([from_str, from_none], obj.get("text"))
        blob = from_union([from_str, from_none], obj.get("blob"))
        return Resource(uri, mime_type, text, blob)

    def to_dict(self) -> dict:
        result: dict = {}
        result["uri"] = from_str(self.uri)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_str, from_none], self.mime_type)
        if self.text is not None:
            result["text"] = from_union([from_str, from_none], self.text)
        if self.blob is not None:
            result["blob"] = from_union([from_str, from_none], self.blob)
        return result


class ContentType(Enum):
    AUDIO = "audio"
    IMAGE = "image"
    RESOURCE = "resource"
    RESOURCE_LINK = "resource_link"
    TERMINAL = "terminal"
    TEXT = "text"


@dataclass
class Content:
    """A content block within a tool result, which may be text, terminal output, image, audio,
    or a resource
    
    Plain text content block
    
    Terminal/shell output content block with optional exit code and working directory
    
    Image content block with base64-encoded data
    
    Audio content block with base64-encoded data
    
    Resource link content block referencing an external resource
    
    Embedded resource content block with inline text or binary data
    """
    type: ContentType
    """Content block type discriminator"""

    text: str | None = None
    """The text content
    
    Terminal/shell output text
    """
    cwd: str | None = None
    """Working directory where the command was executed"""

    exit_code: float | None = None
    """Process exit code, if the command has completed"""

    data: str | None = None
    """Base64-encoded image data
    
    Base64-encoded audio data
    """
    mime_type: str | None = None
    """MIME type of the image (e.g., image/png, image/jpeg)
    
    MIME type of the audio (e.g., audio/wav, audio/mpeg)
    
    MIME type of the resource content
    """
    description: str | None = None
    """Human-readable description of the resource"""

    icons: list[Icon] | None = None
    """Icons associated with this resource"""

    name: str | None = None
    """Resource name identifier"""

    size: float | None = None
    """Size of the resource in bytes"""

    title: str | None = None
    """Human-readable display title for the resource"""

    uri: str | None = None
    """URI identifying the resource"""

    resource: Resource | None = None
    """The embedded resource contents, either text or base64-encoded binary"""

    @staticmethod
    def from_dict(obj: Any) -> 'Content':
        assert isinstance(obj, dict)
        type = ContentType(obj.get("type"))
        text = from_union([from_str, from_none], obj.get("text"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        exit_code = from_union([from_float, from_none], obj.get("exitCode"))
        data = from_union([from_str, from_none], obj.get("data"))
        mime_type = from_union([from_str, from_none], obj.get("mimeType"))
        description = from_union([from_str, from_none], obj.get("description"))
        icons = from_union([lambda x: from_list(Icon.from_dict, x), from_none], obj.get("icons"))
        name = from_union([from_str, from_none], obj.get("name"))
        size = from_union([from_float, from_none], obj.get("size"))
        title = from_union([from_str, from_none], obj.get("title"))
        uri = from_union([from_str, from_none], obj.get("uri"))
        resource = from_union([Resource.from_dict, from_none], obj.get("resource"))
        return Content(type, text, cwd, exit_code, data, mime_type, description, icons, name, size, title, uri, resource)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(ContentType, self.type)
        if self.text is not None:
            result["text"] = from_union([from_str, from_none], self.text)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.exit_code is not None:
            result["exitCode"] = from_union([to_float, from_none], self.exit_code)
        if self.data is not None:
            result["data"] = from_union([from_str, from_none], self.data)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_str, from_none], self.mime_type)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.icons is not None:
            result["icons"] = from_union([lambda x: from_list(lambda x: to_class(Icon, x), x), from_none], self.icons)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.size is not None:
            result["size"] = from_union([to_float, from_none], self.size)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        if self.uri is not None:
            result["uri"] = from_union([from_str, from_none], self.uri)
        if self.resource is not None:
            result["resource"] = from_union([lambda x: to_class(Resource, x), from_none], self.resource)
        return result


class ResultKind(Enum):
    """The outcome of the permission request"""

    APPROVED = "approved"
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"
    DENIED_BY_RULES = "denied-by-rules"
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"


@dataclass
class Result:
    """Tool execution result on success
    
    The result of the permission request
    """
    content: str | None = None
    """Concise tool result text sent to the LLM for chat completion, potentially truncated for
    token efficiency
    """
    contents: list[Content] | None = None
    """Structured content blocks (text, images, audio, resources) returned by the tool in their
    native format
    """
    detailed_content: str | None = None
    """Full detailed tool result for UI/timeline display, preserving complete content such as
    diffs. Falls back to content when absent.
    """
    kind: ResultKind | None = None
    """The outcome of the permission request"""

    @staticmethod
    def from_dict(obj: Any) -> 'Result':
        assert isinstance(obj, dict)
        content = from_union([from_str, from_none], obj.get("content"))
        contents = from_union([lambda x: from_list(Content.from_dict, x), from_none], obj.get("contents"))
        detailed_content = from_union([from_str, from_none], obj.get("detailedContent"))
        kind = from_union([ResultKind, from_none], obj.get("kind"))
        return Result(content, contents, detailed_content, kind)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.content is not None:
            result["content"] = from_union([from_str, from_none], self.content)
        if self.contents is not None:
            result["contents"] = from_union([lambda x: from_list(lambda x: to_class(Content, x), x), from_none], self.contents)
        if self.detailed_content is not None:
            result["detailedContent"] = from_union([from_str, from_none], self.detailed_content)
        if self.kind is not None:
            result["kind"] = from_union([lambda x: to_enum(ResultKind, x), from_none], self.kind)
        return result


class Role(Enum):
    """Message role: "system" for system prompts, "developer" for developer-injected instructions"""

    DEVELOPER = "developer"
    SYSTEM = "system"


class ShutdownType(Enum):
    """Whether the session ended normally ("routine") or due to a crash/fatal error ("error")"""

    ERROR = "error"
    ROUTINE = "routine"


class Source(Enum):
    """Origin of this message, used for timeline filtering and telemetry (e.g., "user",
    "autopilot", "skill", or "command")
    """
    AUTOPILOT = "autopilot"
    COMMAND = "command"
    IMMEDIATE_PROMPT = "immediate-prompt"
    JIT_INSTRUCTION = "jit-instruction"
    OTHER = "other"
    SKILL = "skill"
    SNIPPY_BLOCKING = "snippy-blocking"
    SYSTEM = "system"
    THINKING_EXHAUSTED_CONTINUATION = "thinking-exhausted-continuation"
    USER = "user"


class SourceType(Enum):
    """Origin type of the session being handed off"""

    LOCAL = "local"
    REMOTE = "remote"


class ToolRequestType(Enum):
    """Tool call type: "function" for standard tool calls, "custom" for grammar-based tool
    calls. Defaults to "function" when absent.
    """
    CUSTOM = "custom"
    FUNCTION = "function"


@dataclass
class ToolRequest:
    """A tool invocation request from the assistant"""

    name: str
    """Name of the tool being invoked"""

    tool_call_id: str
    """Unique identifier for this tool call"""

    arguments: Any = None
    """Arguments to pass to the tool, format depends on the tool"""

    intention_summary: str | None = None
    """Resolved intention summary describing what this specific call does"""

    tool_title: str | None = None
    """Human-readable display title for the tool"""

    type: ToolRequestType | None = None
    """Tool call type: "function" for standard tool calls, "custom" for grammar-based tool
    calls. Defaults to "function" when absent.
    """

    @staticmethod
    def from_dict(obj: Any) -> 'ToolRequest':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        tool_call_id = from_str(obj.get("toolCallId"))
        arguments = obj.get("arguments")
        intention_summary = from_union([from_none, from_str], obj.get("intentionSummary"))
        tool_title = from_union([from_str, from_none], obj.get("toolTitle"))
        type = from_union([ToolRequestType, from_none], obj.get("type"))
        return ToolRequest(name, tool_call_id, arguments, intention_summary, tool_title, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.intention_summary is not None:
            result["intentionSummary"] = from_union([from_none, from_str], self.intention_summary)
        if self.tool_title is not None:
            result["toolTitle"] = from_union([from_str, from_none], self.tool_title)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(ToolRequestType, x), from_none], self.type)
        return result


@dataclass
class Data:
    """Session initialization metadata including context and configuration
    
    Session resume metadata including current context and event count
    
    Error details for timeline display including message and optional diagnostic information
    
    Payload indicating the agent is idle; includes any background tasks still in flight
    
    Session title change payload containing the new display title
    
    Informational message for timeline display with categorization
    
    Warning message for timeline display with categorization
    
    Model change details including previous and new model identifiers
    
    Agent mode change details including previous and new modes
    
    Plan file operation details indicating what changed
    
    Workspace file change details including path and operation type
    
    Session handoff metadata including source, context, and repository information
    
    Conversation truncation statistics including token counts and removed content metrics
    
    Session rewind details including target event and count of removed events
    
    Session termination metrics including usage statistics, code changes, and shutdown
    reason
    
    Updated working directory and git context after the change
    
    Current context window usage statistics including token and message counts
    
    Empty payload; the event signals that LLM-powered conversation compaction has begun
    
    Conversation compaction results including success status, metrics, and optional error
    details
    
    Task completion notification with optional summary from the agent
    
    User message content with optional attachments, source information, and interaction
    metadata
    
    Empty payload; the event signals that the pending message queue has changed
    
    Turn initialization metadata including identifier and interaction tracking
    
    Agent intent description for current activity or plan
    
    Assistant reasoning content for timeline display with complete thinking text
    
    Streaming reasoning delta for incremental extended thinking updates
    
    Streaming response progress with cumulative byte count
    
    Assistant response containing text content, optional tool requests, and interaction
    metadata
    
    Streaming assistant message delta for incremental response updates
    
    Turn completion metadata including the turn identifier
    
    LLM API call usage metrics including tokens, costs, quotas, and billing information
    
    Turn abort information including the reason for termination
    
    User-initiated tool invocation request with tool name and arguments
    
    Tool execution startup details including MCP server information when applicable
    
    Streaming tool execution output for incremental result display
    
    Tool execution progress notification with status message
    
    Tool execution completion results including success status, detailed output, and error
    information
    
    Skill invocation details including content, allowed tools, and plugin metadata
    
    Sub-agent startup details including parent tool call and agent information
    
    Sub-agent completion details for successful execution
    
    Sub-agent failure details including error message and agent information
    
    Custom agent selection details including name and available tools
    
    Empty payload; the event signals that the custom agent was deselected, returning to the
    default agent
    
    Hook invocation start details including type and input data
    
    Hook invocation completion details including output, success status, and error
    information
    
    System or developer message content with role and optional template metadata
    
    System-generated notification for runtime events like background task completion
    
    Permission request notification requiring client approval with request details
    
    Permission request completion notification signaling UI dismissal
    
    User input request notification with question and optional predefined choices
    
    User input request completion notification signaling UI dismissal
    
    Structured form elicitation request with JSON schema definition for form fields
    
    Elicitation request completion notification signaling UI dismissal
    
    External tool invocation request for client-side tool execution
    
    External tool completion notification signaling UI dismissal
    
    Queued slash command dispatch request for client execution
    
    Queued command completion notification signaling UI dismissal
    
    Plan approval request with plan content and available user actions
    
    Plan mode exit completion notification signaling UI dismissal
    """
    already_in_use: bool | None = None
    """Whether the session was already in use by another client at start time
    
    Whether the session was already in use by another client at resume time
    """
    context: ContextClass | str | None = None
    """Working directory and git context at session start
    
    Updated working directory and git context at resume time
    
    Additional context information for the handoff
    """
    copilot_version: str | None = None
    """Version string of the Copilot application"""

    producer: str | None = None
    """Identifier of the software producing the events (e.g., "copilot-agent")"""

    reasoning_effort: str | None = None
    """Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high",
    "xhigh")
    
    Reasoning effort level after the model change, if applicable
    """
    selected_model: str | None = None
    """Model selected at session creation time, if any
    
    Model currently selected at resume time
    """
    session_id: str | None = None
    """Unique identifier for the session
    
    Session ID that this external tool request belongs to
    """
    start_time: datetime | None = None
    """ISO 8601 timestamp when the session was created"""

    version: float | None = None
    """Schema version number for the session event format"""

    event_count: float | None = None
    """Total number of persisted events in the session at the time of resume"""

    resume_time: datetime | None = None
    """ISO 8601 timestamp when the session was resumed"""

    error_type: str | None = None
    """Category of error (e.g., "authentication", "authorization", "quota", "rate_limit",
    "query")
    """
    message: str | None = None
    """Human-readable error message
    
    Human-readable informational message for display in the timeline
    
    Human-readable warning message for display in the timeline
    
    Message describing what information is needed from the user
    """
    provider_call_id: str | None = None
    """GitHub request tracing ID (x-github-request-id header) for correlating with server-side
    logs
    
    GitHub request tracing ID (x-github-request-id header) for server-side log correlation
    """
    stack: str | None = None
    """Error stack trace, when available"""

    status_code: int | None = None
    """HTTP status code from the upstream request, if applicable"""

    background_tasks: BackgroundTasks | None = None
    """Background tasks still running when the agent became idle"""

    title: str | None = None
    """The new display title for the session"""

    info_type: str | None = None
    """Category of informational message (e.g., "notification", "timing", "context_window",
    "mcp", "snapshot", "configuration", "authentication", "model")
    """
    warning_type: str | None = None
    """Category of warning (e.g., "subscription", "policy", "mcp")"""

    new_model: str | None = None
    """Newly selected model identifier"""

    previous_model: str | None = None
    """Model that was previously selected, if any"""

    previous_reasoning_effort: str | None = None
    """Reasoning effort level before the model change, if applicable"""

    new_mode: str | None = None
    """Agent mode after the change (e.g., "interactive", "plan", "autopilot")"""

    previous_mode: str | None = None
    """Agent mode before the change (e.g., "interactive", "plan", "autopilot")"""

    operation: Operation | None = None
    """The type of operation performed on the plan file
    
    Whether the file was newly created or updated
    """
    path: str | None = None
    """Relative path within the session workspace files directory
    
    File path to the SKILL.md definition
    """
    handoff_time: datetime | None = None
    """ISO 8601 timestamp when the handoff occurred"""

    remote_session_id: str | None = None
    """Session ID of the remote session being handed off"""

    repository: RepositoryClass | str | None = None
    """Repository context for the handed-off session
    
    Repository identifier derived from the git remote URL ("owner/name" for GitHub,
    "org/project/repo" for Azure DevOps)
    """
    source_type: SourceType | None = None
    """Origin type of the session being handed off"""

    summary: str | None = None
    """Summary of the work done in the source session
    
    Optional summary of the completed task, provided by the agent
    
    Summary of the plan that was created
    """
    messages_removed_during_truncation: float | None = None
    """Number of messages removed by truncation"""

    performed_by: str | None = None
    """Identifier of the component that performed truncation (e.g., "BasicTruncator")"""

    post_truncation_messages_length: float | None = None
    """Number of conversation messages after truncation"""

    post_truncation_tokens_in_messages: float | None = None
    """Total tokens in conversation messages after truncation"""

    pre_truncation_messages_length: float | None = None
    """Number of conversation messages before truncation"""

    pre_truncation_tokens_in_messages: float | None = None
    """Total tokens in conversation messages before truncation"""

    token_limit: float | None = None
    """Maximum token count for the model's context window"""

    tokens_removed_during_truncation: float | None = None
    """Number of tokens removed by truncation"""

    events_removed: float | None = None
    """Number of events that were removed by the rewind"""

    up_to_event_id: str | None = None
    """Event ID that was rewound to; all events after this one were removed"""

    code_changes: CodeChanges | None = None
    """Aggregate code change metrics for the session"""

    current_model: str | None = None
    """Model that was selected at the time of shutdown"""

    error_reason: str | None = None
    """Error description when shutdownType is "error\""""

    model_metrics: dict[str, ModelMetric] | None = None
    """Per-model usage breakdown, keyed by model identifier"""

    session_start_time: float | None = None
    """Unix timestamp (milliseconds) when the session started"""

    shutdown_type: ShutdownType | None = None
    """Whether the session ended normally ("routine") or due to a crash/fatal error ("error")"""

    total_api_duration_ms: float | None = None
    """Cumulative time spent in API calls during the session, in milliseconds"""

    total_premium_requests: float | None = None
    """Total number of premium API requests used during the session"""

    base_commit: str | None = None
    """Base commit of current git branch at session start time"""

    branch: str | None = None
    """Current git branch name"""

    cwd: str | None = None
    """Current working directory path"""

    git_root: str | None = None
    """Root directory of the git repository, resolved via git rev-parse"""

    head_commit: str | None = None
    """Head commit of current git branch at session start time"""

    host_type: HostType | None = None
    """Hosting platform type of the repository (github or ado)"""

    current_tokens: float | None = None
    """Current number of tokens in the context window"""

    messages_length: float | None = None
    """Current number of messages in the conversation"""

    checkpoint_number: float | None = None
    """Checkpoint snapshot number created for recovery"""

    checkpoint_path: str | None = None
    """File path where the checkpoint was stored"""

    compaction_tokens_used: CompactionTokensUsed | None = None
    """Token usage breakdown for the compaction LLM call"""

    error: ErrorClass | str | None = None
    """Error message if compaction failed
    
    Error details when the tool execution failed
    
    Error message describing why the sub-agent failed
    
    Error details when the hook failed
    """
    messages_removed: float | None = None
    """Number of messages removed during compaction"""

    post_compaction_tokens: float | None = None
    """Total tokens in conversation after compaction"""

    pre_compaction_messages_length: float | None = None
    """Number of messages before compaction"""

    pre_compaction_tokens: float | None = None
    """Total tokens in conversation before compaction"""

    request_id: str | None = None
    """GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
    
    Unique identifier for this permission request; used to respond via
    session.respondToPermission()
    
    Request ID of the resolved permission request; clients should dismiss any UI for this
    request
    
    Unique identifier for this input request; used to respond via
    session.respondToUserInput()
    
    Request ID of the resolved user input request; clients should dismiss any UI for this
    request
    
    Unique identifier for this elicitation request; used to respond via
    session.respondToElicitation()
    
    Request ID of the resolved elicitation request; clients should dismiss any UI for this
    request
    
    Unique identifier for this request; used to respond via session.respondToExternalTool()
    
    Request ID of the resolved external tool request; clients should dismiss any UI for this
    request
    
    Unique identifier for this request; used to respond via session.respondToQueuedCommand()
    
    Request ID of the resolved command request; clients should dismiss any UI for this
    request
    
    Unique identifier for this request; used to respond via session.respondToExitPlanMode()
    
    Request ID of the resolved exit plan mode request; clients should dismiss any UI for this
    request
    """
    success: bool | None = None
    """Whether compaction completed successfully
    
    Whether the tool execution completed successfully
    
    Whether the hook completed successfully
    """
    summary_content: str | None = None
    """LLM-generated summary of the compacted conversation history"""

    tokens_removed: float | None = None
    """Number of tokens removed during compaction"""

    agent_mode: AgentMode | None = None
    """The agent mode that was active when this message was sent"""

    attachments: list[Attachment] | None = None
    """Files, selections, or GitHub references attached to the message"""

    content: str | None = None
    """The user's message text as displayed in the timeline
    
    The complete extended thinking text from the model
    
    The assistant's text response content
    
    Full content of the skill file, injected into the conversation for the model
    
    The system or developer prompt text
    
    The notification text, typically wrapped in <system_notification> XML tags
    """
    interaction_id: str | None = None
    """CAPI interaction ID for correlating this user message with its turn
    
    CAPI interaction ID for correlating this turn with upstream telemetry
    
    CAPI interaction ID for correlating this message with upstream telemetry
    
    CAPI interaction ID for correlating this tool execution with upstream telemetry
    """
    source: Source | None = None
    """Origin of this message, used for timeline filtering and telemetry (e.g., "user",
    "autopilot", "skill", or "command")
    """
    transformed_content: str | None = None
    """Transformed version of the message sent to the model, with XML wrapping, timestamps, and
    other augmentations for prompt caching
    """
    turn_id: str | None = None
    """Identifier for this turn within the agentic loop, typically a stringified turn number
    
    Identifier of the turn that has ended, matching the corresponding assistant.turn_start
    event
    """
    intent: str | None = None
    """Short description of what the agent is currently doing or planning to do"""

    reasoning_id: str | None = None
    """Unique identifier for this reasoning block
    
    Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning
    event
    """
    delta_content: str | None = None
    """Incremental text chunk to append to the reasoning content
    
    Incremental text chunk to append to the message content
    """
    total_response_size_bytes: float | None = None
    """Cumulative total bytes received from the streaming response so far"""

    encrypted_content: str | None = None
    """Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume."""

    message_id: str | None = None
    """Unique identifier for this assistant message
    
    Message ID this delta belongs to, matching the corresponding assistant.message event
    """
    output_tokens: float | None = None
    """Actual output token count from the API response (completion_tokens), used for accurate
    token accounting
    
    Number of output tokens produced
    """
    parent_tool_call_id: str | None = None
    """Tool call ID of the parent tool invocation when this event originates from a sub-agent
    
    Parent tool call ID when this usage originates from a sub-agent
    """
    phase: str | None = None
    """Generation phase for phased-output models (e.g., thinking vs. response phases)"""

    reasoning_opaque: str | None = None
    """Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped
    on resume.
    """
    reasoning_text: str | None = None
    """Readable reasoning text from the model's extended thinking"""

    tool_requests: list[ToolRequest] | None = None
    """Tool invocations requested by the assistant in this message"""

    api_call_id: str | None = None
    """Completion ID from the model provider (e.g., chatcmpl-abc123)"""

    cache_read_tokens: float | None = None
    """Number of tokens read from prompt cache"""

    cache_write_tokens: float | None = None
    """Number of tokens written to prompt cache"""

    copilot_usage: CopilotUsage | None = None
    """Per-request cost and usage data from the CAPI copilot_usage response field"""

    cost: float | None = None
    """Model multiplier cost for billing purposes"""

    duration: float | None = None
    """Duration of the API call in milliseconds"""

    initiator: str | None = None
    """What initiated this API call (e.g., "sub-agent"); absent for user-initiated calls"""

    input_tokens: float | None = None
    """Number of input tokens consumed"""

    model: str | None = None
    """Model identifier used for this API call
    
    Model identifier that generated this tool call
    """
    quota_snapshots: dict[str, QuotaSnapshot] | None = None
    """Per-quota resource usage snapshots, keyed by quota identifier"""

    reason: str | None = None
    """Reason the current turn was aborted (e.g., "user initiated")"""

    arguments: Any = None
    """Arguments for the tool invocation
    
    Arguments passed to the tool
    
    Arguments to pass to the external tool
    """
    tool_call_id: str | None = None
    """Unique identifier for this tool call
    
    Tool call ID this partial result belongs to
    
    Tool call ID this progress notification belongs to
    
    Unique identifier for the completed tool call
    
    Tool call ID of the parent tool invocation that spawned this sub-agent
    
    Tool call ID assigned to this external tool invocation
    """
    tool_name: str | None = None
    """Name of the tool the user wants to invoke
    
    Name of the tool being executed
    
    Name of the external tool to invoke
    """
    mcp_server_name: str | None = None
    """Name of the MCP server hosting this tool, when the tool is an MCP tool"""

    mcp_tool_name: str | None = None
    """Original tool name on the MCP server, when the tool is an MCP tool"""

    partial_output: str | None = None
    """Incremental output chunk from the running tool"""

    progress_message: str | None = None
    """Human-readable progress status message (e.g., from an MCP server)"""

    is_user_requested: bool | None = None
    """Whether this tool call was explicitly requested by the user rather than the assistant"""

    result: Result | None = None
    """Tool execution result on success
    
    The result of the permission request
    """
    tool_telemetry: dict[str, Any] | None = None
    """Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)"""

    allowed_tools: list[str] | None = None
    """Tool names that should be auto-approved when this skill is active"""

    name: str | None = None
    """Name of the invoked skill
    
    Optional name identifier for the message source
    """
    plugin_name: str | None = None
    """Name of the plugin this skill originated from, when applicable"""

    plugin_version: str | None = None
    """Version of the plugin this skill originated from, when applicable"""

    agent_description: str | None = None
    """Description of what the sub-agent does"""

    agent_display_name: str | None = None
    """Human-readable display name of the sub-agent
    
    Human-readable display name of the selected custom agent
    """
    agent_name: str | None = None
    """Internal name of the sub-agent
    
    Internal name of the selected custom agent
    """
    tools: list[str] | None = None
    """List of tool names available to this agent, or null for all tools"""

    hook_invocation_id: str | None = None
    """Unique identifier for this hook invocation
    
    Identifier matching the corresponding hook.start event
    """
    hook_type: str | None = None
    """Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
    
    Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
    """
    input: Any = None
    """Input data passed to the hook"""

    output: Any = None
    """Output data produced by the hook"""

    metadata: Metadata | None = None
    """Metadata about the prompt template and its construction"""

    role: Role | None = None
    """Message role: "system" for system prompts, "developer" for developer-injected instructions"""

    kind: KindClass | None = None
    """Structured metadata identifying what triggered this notification"""

    permission_request: PermissionRequest | None = None
    """Details of the permission being requested"""

    allow_freeform: bool | None = None
    """Whether the user can provide a free-form text response in addition to predefined choices"""

    choices: list[str] | None = None
    """Predefined choices for the user to select from, if applicable"""

    question: str | None = None
    """The question or prompt to present to the user"""

    mode: Mode | None = None
    """Elicitation mode; currently only "form" is supported. Defaults to "form" when absent."""

    requested_schema: RequestedSchema | None = None
    """JSON Schema describing the form fields to present to the user"""

    traceparent: str | None = None
    """W3C Trace Context traceparent header for the execute_tool span"""

    tracestate: str | None = None
    """W3C Trace Context tracestate header for the execute_tool span"""

    command: str | None = None
    """The slash command text to be executed (e.g., /help, /clear)"""

    actions: list[str] | None = None
    """Available actions the user can take (e.g., approve, edit, reject)"""

    plan_content: str | None = None
    """Full content of the plan file"""

    recommended_action: str | None = None
    """The recommended action for the user to take"""

    @staticmethod
    def from_dict(obj: Any) -> 'Data':
        assert isinstance(obj, dict)
        already_in_use = from_union([from_bool, from_none], obj.get("alreadyInUse"))
        context = from_union([ContextClass.from_dict, from_str, from_none], obj.get("context"))
        copilot_version = from_union([from_str, from_none], obj.get("copilotVersion"))
        producer = from_union([from_str, from_none], obj.get("producer"))
        reasoning_effort = from_union([from_str, from_none], obj.get("reasoningEffort"))
        selected_model = from_union([from_str, from_none], obj.get("selectedModel"))
        session_id = from_union([from_str, from_none], obj.get("sessionId"))
        start_time = from_union([from_datetime, from_none], obj.get("startTime"))
        version = from_union([from_float, from_none], obj.get("version"))
        event_count = from_union([from_float, from_none], obj.get("eventCount"))
        resume_time = from_union([from_datetime, from_none], obj.get("resumeTime"))
        error_type = from_union([from_str, from_none], obj.get("errorType"))
        message = from_union([from_str, from_none], obj.get("message"))
        provider_call_id = from_union([from_str, from_none], obj.get("providerCallId"))
        stack = from_union([from_str, from_none], obj.get("stack"))
        status_code = from_union([from_int, from_none], obj.get("statusCode"))
        background_tasks = from_union([BackgroundTasks.from_dict, from_none], obj.get("backgroundTasks"))
        title = from_union([from_str, from_none], obj.get("title"))
        info_type = from_union([from_str, from_none], obj.get("infoType"))
        warning_type = from_union([from_str, from_none], obj.get("warningType"))
        new_model = from_union([from_str, from_none], obj.get("newModel"))
        previous_model = from_union([from_str, from_none], obj.get("previousModel"))
        previous_reasoning_effort = from_union([from_str, from_none], obj.get("previousReasoningEffort"))
        new_mode = from_union([from_str, from_none], obj.get("newMode"))
        previous_mode = from_union([from_str, from_none], obj.get("previousMode"))
        operation = from_union([Operation, from_none], obj.get("operation"))
        path = from_union([from_str, from_none], obj.get("path"))
        handoff_time = from_union([from_datetime, from_none], obj.get("handoffTime"))
        remote_session_id = from_union([from_str, from_none], obj.get("remoteSessionId"))
        repository = from_union([RepositoryClass.from_dict, from_str, from_none], obj.get("repository"))
        source_type = from_union([SourceType, from_none], obj.get("sourceType"))
        summary = from_union([from_str, from_none], obj.get("summary"))
        messages_removed_during_truncation = from_union([from_float, from_none], obj.get("messagesRemovedDuringTruncation"))
        performed_by = from_union([from_str, from_none], obj.get("performedBy"))
        post_truncation_messages_length = from_union([from_float, from_none], obj.get("postTruncationMessagesLength"))
        post_truncation_tokens_in_messages = from_union([from_float, from_none], obj.get("postTruncationTokensInMessages"))
        pre_truncation_messages_length = from_union([from_float, from_none], obj.get("preTruncationMessagesLength"))
        pre_truncation_tokens_in_messages = from_union([from_float, from_none], obj.get("preTruncationTokensInMessages"))
        token_limit = from_union([from_float, from_none], obj.get("tokenLimit"))
        tokens_removed_during_truncation = from_union([from_float, from_none], obj.get("tokensRemovedDuringTruncation"))
        events_removed = from_union([from_float, from_none], obj.get("eventsRemoved"))
        up_to_event_id = from_union([from_str, from_none], obj.get("upToEventId"))
        code_changes = from_union([CodeChanges.from_dict, from_none], obj.get("codeChanges"))
        current_model = from_union([from_str, from_none], obj.get("currentModel"))
        error_reason = from_union([from_str, from_none], obj.get("errorReason"))
        model_metrics = from_union([lambda x: from_dict(ModelMetric.from_dict, x), from_none], obj.get("modelMetrics"))
        session_start_time = from_union([from_float, from_none], obj.get("sessionStartTime"))
        shutdown_type = from_union([ShutdownType, from_none], obj.get("shutdownType"))
        total_api_duration_ms = from_union([from_float, from_none], obj.get("totalApiDurationMs"))
        total_premium_requests = from_union([from_float, from_none], obj.get("totalPremiumRequests"))
        base_commit = from_union([from_str, from_none], obj.get("baseCommit"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        git_root = from_union([from_str, from_none], obj.get("gitRoot"))
        head_commit = from_union([from_str, from_none], obj.get("headCommit"))
        host_type = from_union([HostType, from_none], obj.get("hostType"))
        current_tokens = from_union([from_float, from_none], obj.get("currentTokens"))
        messages_length = from_union([from_float, from_none], obj.get("messagesLength"))
        checkpoint_number = from_union([from_float, from_none], obj.get("checkpointNumber"))
        checkpoint_path = from_union([from_str, from_none], obj.get("checkpointPath"))
        compaction_tokens_used = from_union([CompactionTokensUsed.from_dict, from_none], obj.get("compactionTokensUsed"))
        error = from_union([ErrorClass.from_dict, from_str, from_none], obj.get("error"))
        messages_removed = from_union([from_float, from_none], obj.get("messagesRemoved"))
        post_compaction_tokens = from_union([from_float, from_none], obj.get("postCompactionTokens"))
        pre_compaction_messages_length = from_union([from_float, from_none], obj.get("preCompactionMessagesLength"))
        pre_compaction_tokens = from_union([from_float, from_none], obj.get("preCompactionTokens"))
        request_id = from_union([from_str, from_none], obj.get("requestId"))
        success = from_union([from_bool, from_none], obj.get("success"))
        summary_content = from_union([from_str, from_none], obj.get("summaryContent"))
        tokens_removed = from_union([from_float, from_none], obj.get("tokensRemoved"))
        agent_mode = from_union([AgentMode, from_none], obj.get("agentMode"))
        attachments = from_union([lambda x: from_list(Attachment.from_dict, x), from_none], obj.get("attachments"))
        content = from_union([from_str, from_none], obj.get("content"))
        interaction_id = from_union([from_str, from_none], obj.get("interactionId"))
        source = from_union([Source, from_none], obj.get("source"))
        transformed_content = from_union([from_str, from_none], obj.get("transformedContent"))
        turn_id = from_union([from_str, from_none], obj.get("turnId"))
        intent = from_union([from_str, from_none], obj.get("intent"))
        reasoning_id = from_union([from_str, from_none], obj.get("reasoningId"))
        delta_content = from_union([from_str, from_none], obj.get("deltaContent"))
        total_response_size_bytes = from_union([from_float, from_none], obj.get("totalResponseSizeBytes"))
        encrypted_content = from_union([from_str, from_none], obj.get("encryptedContent"))
        message_id = from_union([from_str, from_none], obj.get("messageId"))
        output_tokens = from_union([from_float, from_none], obj.get("outputTokens"))
        parent_tool_call_id = from_union([from_str, from_none], obj.get("parentToolCallId"))
        phase = from_union([from_str, from_none], obj.get("phase"))
        reasoning_opaque = from_union([from_str, from_none], obj.get("reasoningOpaque"))
        reasoning_text = from_union([from_str, from_none], obj.get("reasoningText"))
        tool_requests = from_union([lambda x: from_list(ToolRequest.from_dict, x), from_none], obj.get("toolRequests"))
        api_call_id = from_union([from_str, from_none], obj.get("apiCallId"))
        cache_read_tokens = from_union([from_float, from_none], obj.get("cacheReadTokens"))
        cache_write_tokens = from_union([from_float, from_none], obj.get("cacheWriteTokens"))
        copilot_usage = from_union([CopilotUsage.from_dict, from_none], obj.get("copilotUsage"))
        cost = from_union([from_float, from_none], obj.get("cost"))
        duration = from_union([from_float, from_none], obj.get("duration"))
        initiator = from_union([from_str, from_none], obj.get("initiator"))
        input_tokens = from_union([from_float, from_none], obj.get("inputTokens"))
        model = from_union([from_str, from_none], obj.get("model"))
        quota_snapshots = from_union([lambda x: from_dict(QuotaSnapshot.from_dict, x), from_none], obj.get("quotaSnapshots"))
        reason = from_union([from_str, from_none], obj.get("reason"))
        arguments = obj.get("arguments")
        tool_call_id = from_union([from_str, from_none], obj.get("toolCallId"))
        tool_name = from_union([from_str, from_none], obj.get("toolName"))
        mcp_server_name = from_union([from_str, from_none], obj.get("mcpServerName"))
        mcp_tool_name = from_union([from_str, from_none], obj.get("mcpToolName"))
        partial_output = from_union([from_str, from_none], obj.get("partialOutput"))
        progress_message = from_union([from_str, from_none], obj.get("progressMessage"))
        is_user_requested = from_union([from_bool, from_none], obj.get("isUserRequested"))
        result = from_union([Result.from_dict, from_none], obj.get("result"))
        tool_telemetry = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("toolTelemetry"))
        allowed_tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("allowedTools"))
        name = from_union([from_str, from_none], obj.get("name"))
        plugin_name = from_union([from_str, from_none], obj.get("pluginName"))
        plugin_version = from_union([from_str, from_none], obj.get("pluginVersion"))
        agent_description = from_union([from_str, from_none], obj.get("agentDescription"))
        agent_display_name = from_union([from_str, from_none], obj.get("agentDisplayName"))
        agent_name = from_union([from_str, from_none], obj.get("agentName"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        hook_invocation_id = from_union([from_str, from_none], obj.get("hookInvocationId"))
        hook_type = from_union([from_str, from_none], obj.get("hookType"))
        input = obj.get("input")
        output = obj.get("output")
        metadata = from_union([Metadata.from_dict, from_none], obj.get("metadata"))
        role = from_union([Role, from_none], obj.get("role"))
        kind = from_union([KindClass.from_dict, from_none], obj.get("kind"))
        permission_request = from_union([PermissionRequest.from_dict, from_none], obj.get("permissionRequest"))
        allow_freeform = from_union([from_bool, from_none], obj.get("allowFreeform"))
        choices = from_union([lambda x: from_list(from_str, x), from_none], obj.get("choices"))
        question = from_union([from_str, from_none], obj.get("question"))
        mode = from_union([Mode, from_none], obj.get("mode"))
        requested_schema = from_union([RequestedSchema.from_dict, from_none], obj.get("requestedSchema"))
        traceparent = from_union([from_str, from_none], obj.get("traceparent"))
        tracestate = from_union([from_str, from_none], obj.get("tracestate"))
        command = from_union([from_str, from_none], obj.get("command"))
        actions = from_union([lambda x: from_list(from_str, x), from_none], obj.get("actions"))
        plan_content = from_union([from_str, from_none], obj.get("planContent"))
        recommended_action = from_union([from_str, from_none], obj.get("recommendedAction"))
        return Data(already_in_use, context, copilot_version, producer, reasoning_effort, selected_model, session_id, start_time, version, event_count, resume_time, error_type, message, provider_call_id, stack, status_code, background_tasks, title, info_type, warning_type, new_model, previous_model, previous_reasoning_effort, new_mode, previous_mode, operation, path, handoff_time, remote_session_id, repository, source_type, summary, messages_removed_during_truncation, performed_by, post_truncation_messages_length, post_truncation_tokens_in_messages, pre_truncation_messages_length, pre_truncation_tokens_in_messages, token_limit, tokens_removed_during_truncation, events_removed, up_to_event_id, code_changes, current_model, error_reason, model_metrics, session_start_time, shutdown_type, total_api_duration_ms, total_premium_requests, base_commit, branch, cwd, git_root, head_commit, host_type, current_tokens, messages_length, checkpoint_number, checkpoint_path, compaction_tokens_used, error, messages_removed, post_compaction_tokens, pre_compaction_messages_length, pre_compaction_tokens, request_id, success, summary_content, tokens_removed, agent_mode, attachments, content, interaction_id, source, transformed_content, turn_id, intent, reasoning_id, delta_content, total_response_size_bytes, encrypted_content, message_id, output_tokens, parent_tool_call_id, phase, reasoning_opaque, reasoning_text, tool_requests, api_call_id, cache_read_tokens, cache_write_tokens, copilot_usage, cost, duration, initiator, input_tokens, model, quota_snapshots, reason, arguments, tool_call_id, tool_name, mcp_server_name, mcp_tool_name, partial_output, progress_message, is_user_requested, result, tool_telemetry, allowed_tools, name, plugin_name, plugin_version, agent_description, agent_display_name, agent_name, tools, hook_invocation_id, hook_type, input, output, metadata, role, kind, permission_request, allow_freeform, choices, question, mode, requested_schema, traceparent, tracestate, command, actions, plan_content, recommended_action)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.already_in_use is not None:
            result["alreadyInUse"] = from_union([from_bool, from_none], self.already_in_use)
        if self.context is not None:
            result["context"] = from_union([lambda x: to_class(ContextClass, x), from_str, from_none], self.context)
        if self.copilot_version is not None:
            result["copilotVersion"] = from_union([from_str, from_none], self.copilot_version)
        if self.producer is not None:
            result["producer"] = from_union([from_str, from_none], self.producer)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_str, from_none], self.reasoning_effort)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_str, from_none], self.selected_model)
        if self.session_id is not None:
            result["sessionId"] = from_union([from_str, from_none], self.session_id)
        if self.start_time is not None:
            result["startTime"] = from_union([lambda x: x.isoformat(), from_none], self.start_time)
        if self.version is not None:
            result["version"] = from_union([to_float, from_none], self.version)
        if self.event_count is not None:
            result["eventCount"] = from_union([to_float, from_none], self.event_count)
        if self.resume_time is not None:
            result["resumeTime"] = from_union([lambda x: x.isoformat(), from_none], self.resume_time)
        if self.error_type is not None:
            result["errorType"] = from_union([from_str, from_none], self.error_type)
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_str, from_none], self.provider_call_id)
        if self.stack is not None:
            result["stack"] = from_union([from_str, from_none], self.stack)
        if self.status_code is not None:
            result["statusCode"] = from_union([from_int, from_none], self.status_code)
        if self.background_tasks is not None:
            result["backgroundTasks"] = from_union([lambda x: to_class(BackgroundTasks, x), from_none], self.background_tasks)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        if self.info_type is not None:
            result["infoType"] = from_union([from_str, from_none], self.info_type)
        if self.warning_type is not None:
            result["warningType"] = from_union([from_str, from_none], self.warning_type)
        if self.new_model is not None:
            result["newModel"] = from_union([from_str, from_none], self.new_model)
        if self.previous_model is not None:
            result["previousModel"] = from_union([from_str, from_none], self.previous_model)
        if self.previous_reasoning_effort is not None:
            result["previousReasoningEffort"] = from_union([from_str, from_none], self.previous_reasoning_effort)
        if self.new_mode is not None:
            result["newMode"] = from_union([from_str, from_none], self.new_mode)
        if self.previous_mode is not None:
            result["previousMode"] = from_union([from_str, from_none], self.previous_mode)
        if self.operation is not None:
            result["operation"] = from_union([lambda x: to_enum(Operation, x), from_none], self.operation)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.handoff_time is not None:
            result["handoffTime"] = from_union([lambda x: x.isoformat(), from_none], self.handoff_time)
        if self.remote_session_id is not None:
            result["remoteSessionId"] = from_union([from_str, from_none], self.remote_session_id)
        if self.repository is not None:
            result["repository"] = from_union([lambda x: to_class(RepositoryClass, x), from_str, from_none], self.repository)
        if self.source_type is not None:
            result["sourceType"] = from_union([lambda x: to_enum(SourceType, x), from_none], self.source_type)
        if self.summary is not None:
            result["summary"] = from_union([from_str, from_none], self.summary)
        if self.messages_removed_during_truncation is not None:
            result["messagesRemovedDuringTruncation"] = from_union([to_float, from_none], self.messages_removed_during_truncation)
        if self.performed_by is not None:
            result["performedBy"] = from_union([from_str, from_none], self.performed_by)
        if self.post_truncation_messages_length is not None:
            result["postTruncationMessagesLength"] = from_union([to_float, from_none], self.post_truncation_messages_length)
        if self.post_truncation_tokens_in_messages is not None:
            result["postTruncationTokensInMessages"] = from_union([to_float, from_none], self.post_truncation_tokens_in_messages)
        if self.pre_truncation_messages_length is not None:
            result["preTruncationMessagesLength"] = from_union([to_float, from_none], self.pre_truncation_messages_length)
        if self.pre_truncation_tokens_in_messages is not None:
            result["preTruncationTokensInMessages"] = from_union([to_float, from_none], self.pre_truncation_tokens_in_messages)
        if self.token_limit is not None:
            result["tokenLimit"] = from_union([to_float, from_none], self.token_limit)
        if self.tokens_removed_during_truncation is not None:
            result["tokensRemovedDuringTruncation"] = from_union([to_float, from_none], self.tokens_removed_during_truncation)
        if self.events_removed is not None:
            result["eventsRemoved"] = from_union([to_float, from_none], self.events_removed)
        if self.up_to_event_id is not None:
            result["upToEventId"] = from_union([from_str, from_none], self.up_to_event_id)
        if self.code_changes is not None:
            result["codeChanges"] = from_union([lambda x: to_class(CodeChanges, x), from_none], self.code_changes)
        if self.current_model is not None:
            result["currentModel"] = from_union([from_str, from_none], self.current_model)
        if self.error_reason is not None:
            result["errorReason"] = from_union([from_str, from_none], self.error_reason)
        if self.model_metrics is not None:
            result["modelMetrics"] = from_union([lambda x: from_dict(lambda x: to_class(ModelMetric, x), x), from_none], self.model_metrics)
        if self.session_start_time is not None:
            result["sessionStartTime"] = from_union([to_float, from_none], self.session_start_time)
        if self.shutdown_type is not None:
            result["shutdownType"] = from_union([lambda x: to_enum(ShutdownType, x), from_none], self.shutdown_type)
        if self.total_api_duration_ms is not None:
            result["totalApiDurationMs"] = from_union([to_float, from_none], self.total_api_duration_ms)
        if self.total_premium_requests is not None:
            result["totalPremiumRequests"] = from_union([to_float, from_none], self.total_premium_requests)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_str, from_none], self.base_commit)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_str, from_none], self.git_root)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_str, from_none], self.head_commit)
        if self.host_type is not None:
            result["hostType"] = from_union([lambda x: to_enum(HostType, x), from_none], self.host_type)
        if self.current_tokens is not None:
            result["currentTokens"] = from_union([to_float, from_none], self.current_tokens)
        if self.messages_length is not None:
            result["messagesLength"] = from_union([to_float, from_none], self.messages_length)
        if self.checkpoint_number is not None:
            result["checkpointNumber"] = from_union([to_float, from_none], self.checkpoint_number)
        if self.checkpoint_path is not None:
            result["checkpointPath"] = from_union([from_str, from_none], self.checkpoint_path)
        if self.compaction_tokens_used is not None:
            result["compactionTokensUsed"] = from_union([lambda x: to_class(CompactionTokensUsed, x), from_none], self.compaction_tokens_used)
        if self.error is not None:
            result["error"] = from_union([lambda x: to_class(ErrorClass, x), from_str, from_none], self.error)
        if self.messages_removed is not None:
            result["messagesRemoved"] = from_union([to_float, from_none], self.messages_removed)
        if self.post_compaction_tokens is not None:
            result["postCompactionTokens"] = from_union([to_float, from_none], self.post_compaction_tokens)
        if self.pre_compaction_messages_length is not None:
            result["preCompactionMessagesLength"] = from_union([to_float, from_none], self.pre_compaction_messages_length)
        if self.pre_compaction_tokens is not None:
            result["preCompactionTokens"] = from_union([to_float, from_none], self.pre_compaction_tokens)
        if self.request_id is not None:
            result["requestId"] = from_union([from_str, from_none], self.request_id)
        if self.success is not None:
            result["success"] = from_union([from_bool, from_none], self.success)
        if self.summary_content is not None:
            result["summaryContent"] = from_union([from_str, from_none], self.summary_content)
        if self.tokens_removed is not None:
            result["tokensRemoved"] = from_union([to_float, from_none], self.tokens_removed)
        if self.agent_mode is not None:
            result["agentMode"] = from_union([lambda x: to_enum(AgentMode, x), from_none], self.agent_mode)
        if self.attachments is not None:
            result["attachments"] = from_union([lambda x: from_list(lambda x: to_class(Attachment, x), x), from_none], self.attachments)
        if self.content is not None:
            result["content"] = from_union([from_str, from_none], self.content)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_str, from_none], self.interaction_id)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_enum(Source, x), from_none], self.source)
        if self.transformed_content is not None:
            result["transformedContent"] = from_union([from_str, from_none], self.transformed_content)
        if self.turn_id is not None:
            result["turnId"] = from_union([from_str, from_none], self.turn_id)
        if self.intent is not None:
            result["intent"] = from_union([from_str, from_none], self.intent)
        if self.reasoning_id is not None:
            result["reasoningId"] = from_union([from_str, from_none], self.reasoning_id)
        if self.delta_content is not None:
            result["deltaContent"] = from_union([from_str, from_none], self.delta_content)
        if self.total_response_size_bytes is not None:
            result["totalResponseSizeBytes"] = from_union([to_float, from_none], self.total_response_size_bytes)
        if self.encrypted_content is not None:
            result["encryptedContent"] = from_union([from_str, from_none], self.encrypted_content)
        if self.message_id is not None:
            result["messageId"] = from_union([from_str, from_none], self.message_id)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([to_float, from_none], self.output_tokens)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_str, from_none], self.parent_tool_call_id)
        if self.phase is not None:
            result["phase"] = from_union([from_str, from_none], self.phase)
        if self.reasoning_opaque is not None:
            result["reasoningOpaque"] = from_union([from_str, from_none], self.reasoning_opaque)
        if self.reasoning_text is not None:
            result["reasoningText"] = from_union([from_str, from_none], self.reasoning_text)
        if self.tool_requests is not None:
            result["toolRequests"] = from_union([lambda x: from_list(lambda x: to_class(ToolRequest, x), x), from_none], self.tool_requests)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_str, from_none], self.api_call_id)
        if self.cache_read_tokens is not None:
            result["cacheReadTokens"] = from_union([to_float, from_none], self.cache_read_tokens)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([to_float, from_none], self.cache_write_tokens)
        if self.copilot_usage is not None:
            result["copilotUsage"] = from_union([lambda x: to_class(CopilotUsage, x), from_none], self.copilot_usage)
        if self.cost is not None:
            result["cost"] = from_union([to_float, from_none], self.cost)
        if self.duration is not None:
            result["duration"] = from_union([to_float, from_none], self.duration)
        if self.initiator is not None:
            result["initiator"] = from_union([from_str, from_none], self.initiator)
        if self.input_tokens is not None:
            result["inputTokens"] = from_union([to_float, from_none], self.input_tokens)
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        if self.quota_snapshots is not None:
            result["quotaSnapshots"] = from_union([lambda x: from_dict(lambda x: to_class(QuotaSnapshot, x), x), from_none], self.quota_snapshots)
        if self.reason is not None:
            result["reason"] = from_union([from_str, from_none], self.reason)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_str, from_none], self.tool_call_id)
        if self.tool_name is not None:
            result["toolName"] = from_union([from_str, from_none], self.tool_name)
        if self.mcp_server_name is not None:
            result["mcpServerName"] = from_union([from_str, from_none], self.mcp_server_name)
        if self.mcp_tool_name is not None:
            result["mcpToolName"] = from_union([from_str, from_none], self.mcp_tool_name)
        if self.partial_output is not None:
            result["partialOutput"] = from_union([from_str, from_none], self.partial_output)
        if self.progress_message is not None:
            result["progressMessage"] = from_union([from_str, from_none], self.progress_message)
        if self.is_user_requested is not None:
            result["isUserRequested"] = from_union([from_bool, from_none], self.is_user_requested)
        if self.result is not None:
            result["result"] = from_union([lambda x: to_class(Result, x), from_none], self.result)
        if self.tool_telemetry is not None:
            result["toolTelemetry"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.tool_telemetry)
        if self.allowed_tools is not None:
            result["allowedTools"] = from_union([lambda x: from_list(from_str, x), from_none], self.allowed_tools)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.plugin_name is not None:
            result["pluginName"] = from_union([from_str, from_none], self.plugin_name)
        if self.plugin_version is not None:
            result["pluginVersion"] = from_union([from_str, from_none], self.plugin_version)
        if self.agent_description is not None:
            result["agentDescription"] = from_union([from_str, from_none], self.agent_description)
        if self.agent_display_name is not None:
            result["agentDisplayName"] = from_union([from_str, from_none], self.agent_display_name)
        if self.agent_name is not None:
            result["agentName"] = from_union([from_str, from_none], self.agent_name)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.hook_invocation_id is not None:
            result["hookInvocationId"] = from_union([from_str, from_none], self.hook_invocation_id)
        if self.hook_type is not None:
            result["hookType"] = from_union([from_str, from_none], self.hook_type)
        if self.input is not None:
            result["input"] = self.input
        if self.output is not None:
            result["output"] = self.output
        if self.metadata is not None:
            result["metadata"] = from_union([lambda x: to_class(Metadata, x), from_none], self.metadata)
        if self.role is not None:
            result["role"] = from_union([lambda x: to_enum(Role, x), from_none], self.role)
        if self.kind is not None:
            result["kind"] = from_union([lambda x: to_class(KindClass, x), from_none], self.kind)
        if self.permission_request is not None:
            result["permissionRequest"] = from_union([lambda x: to_class(PermissionRequest, x), from_none], self.permission_request)
        if self.allow_freeform is not None:
            result["allowFreeform"] = from_union([from_bool, from_none], self.allow_freeform)
        if self.choices is not None:
            result["choices"] = from_union([lambda x: from_list(from_str, x), from_none], self.choices)
        if self.question is not None:
            result["question"] = from_union([from_str, from_none], self.question)
        if self.mode is not None:
            result["mode"] = from_union([lambda x: to_enum(Mode, x), from_none], self.mode)
        if self.requested_schema is not None:
            result["requestedSchema"] = from_union([lambda x: to_class(RequestedSchema, x), from_none], self.requested_schema)
        if self.traceparent is not None:
            result["traceparent"] = from_union([from_str, from_none], self.traceparent)
        if self.tracestate is not None:
            result["tracestate"] = from_union([from_str, from_none], self.tracestate)
        if self.command is not None:
            result["command"] = from_union([from_str, from_none], self.command)
        if self.actions is not None:
            result["actions"] = from_union([lambda x: from_list(from_str, x), from_none], self.actions)
        if self.plan_content is not None:
            result["planContent"] = from_union([from_str, from_none], self.plan_content)
        if self.recommended_action is not None:
            result["recommendedAction"] = from_union([from_str, from_none], self.recommended_action)
        return result


class SessionEventType(Enum):
    ABORT = "abort"
    ASSISTANT_INTENT = "assistant.intent"
    ASSISTANT_MESSAGE = "assistant.message"
    ASSISTANT_MESSAGE_DELTA = "assistant.message_delta"
    ASSISTANT_REASONING = "assistant.reasoning"
    ASSISTANT_REASONING_DELTA = "assistant.reasoning_delta"
    ASSISTANT_STREAMING_DELTA = "assistant.streaming_delta"
    ASSISTANT_TURN_END = "assistant.turn_end"
    ASSISTANT_TURN_START = "assistant.turn_start"
    ASSISTANT_USAGE = "assistant.usage"
    COMMAND_COMPLETED = "command.completed"
    COMMAND_QUEUED = "command.queued"
    ELICITATION_COMPLETED = "elicitation.completed"
    ELICITATION_REQUESTED = "elicitation.requested"
    EXIT_PLAN_MODE_COMPLETED = "exit_plan_mode.completed"
    EXIT_PLAN_MODE_REQUESTED = "exit_plan_mode.requested"
    EXTERNAL_TOOL_COMPLETED = "external_tool.completed"
    EXTERNAL_TOOL_REQUESTED = "external_tool.requested"
    HOOK_END = "hook.end"
    HOOK_START = "hook.start"
    PENDING_MESSAGES_MODIFIED = "pending_messages.modified"
    PERMISSION_COMPLETED = "permission.completed"
    PERMISSION_REQUESTED = "permission.requested"
    SESSION_BACKGROUND_TASKS_CHANGED = "session.background_tasks_changed"
    SESSION_COMPACTION_COMPLETE = "session.compaction_complete"
    SESSION_COMPACTION_START = "session.compaction_start"
    SESSION_CONTEXT_CHANGED = "session.context_changed"
    SESSION_ERROR = "session.error"
    SESSION_HANDOFF = "session.handoff"
    SESSION_IDLE = "session.idle"
    SESSION_INFO = "session.info"
    SESSION_MODEL_CHANGE = "session.model_change"
    SESSION_MODE_CHANGED = "session.mode_changed"
    SESSION_PLAN_CHANGED = "session.plan_changed"
    SESSION_RESUME = "session.resume"
    SESSION_SHUTDOWN = "session.shutdown"
    SESSION_SNAPSHOT_REWIND = "session.snapshot_rewind"
    SESSION_START = "session.start"
    SESSION_TASK_COMPLETE = "session.task_complete"
    SESSION_TITLE_CHANGED = "session.title_changed"
    SESSION_TOOLS_UPDATED = "session.tools_updated"
    SESSION_TRUNCATION = "session.truncation"
    SESSION_USAGE_INFO = "session.usage_info"
    SESSION_WARNING = "session.warning"
    SESSION_WORKSPACE_FILE_CHANGED = "session.workspace_file_changed"
    SKILL_INVOKED = "skill.invoked"
    SUBAGENT_COMPLETED = "subagent.completed"
    SUBAGENT_DESELECTED = "subagent.deselected"
    SUBAGENT_FAILED = "subagent.failed"
    SUBAGENT_SELECTED = "subagent.selected"
    SUBAGENT_STARTED = "subagent.started"
    SYSTEM_MESSAGE = "system.message"
    SYSTEM_NOTIFICATION = "system.notification"
    TOOL_EXECUTION_COMPLETE = "tool.execution_complete"
    TOOL_EXECUTION_PARTIAL_RESULT = "tool.execution_partial_result"
    TOOL_EXECUTION_PROGRESS = "tool.execution_progress"
    TOOL_EXECUTION_START = "tool.execution_start"
    TOOL_USER_REQUESTED = "tool.user_requested"
    USER_INPUT_COMPLETED = "user_input.completed"
    USER_INPUT_REQUESTED = "user_input.requested"
    USER_MESSAGE = "user.message"
    # UNKNOWN is used for forward compatibility
    UNKNOWN = "unknown"

    @classmethod
    def _missing_(cls, value: object) -> "SessionEventType":
        """Handle unknown event types gracefully for forward compatibility."""
        return cls.UNKNOWN



@dataclass
class SessionEvent:
    data: Data
    """Session initialization metadata including context and configuration
    
    Session resume metadata including current context and event count
    
    Error details for timeline display including message and optional diagnostic information
    
    Payload indicating the agent is idle; includes any background tasks still in flight
    
    Session title change payload containing the new display title
    
    Informational message for timeline display with categorization
    
    Warning message for timeline display with categorization
    
    Model change details including previous and new model identifiers
    
    Agent mode change details including previous and new modes
    
    Plan file operation details indicating what changed
    
    Workspace file change details including path and operation type
    
    Session handoff metadata including source, context, and repository information
    
    Conversation truncation statistics including token counts and removed content metrics
    
    Session rewind details including target event and count of removed events
    
    Session termination metrics including usage statistics, code changes, and shutdown
    reason
    
    Updated working directory and git context after the change
    
    Current context window usage statistics including token and message counts
    
    Empty payload; the event signals that LLM-powered conversation compaction has begun
    
    Conversation compaction results including success status, metrics, and optional error
    details
    
    Task completion notification with optional summary from the agent
    
    User message content with optional attachments, source information, and interaction
    metadata
    
    Empty payload; the event signals that the pending message queue has changed
    
    Turn initialization metadata including identifier and interaction tracking
    
    Agent intent description for current activity or plan
    
    Assistant reasoning content for timeline display with complete thinking text
    
    Streaming reasoning delta for incremental extended thinking updates
    
    Streaming response progress with cumulative byte count
    
    Assistant response containing text content, optional tool requests, and interaction
    metadata
    
    Streaming assistant message delta for incremental response updates
    
    Turn completion metadata including the turn identifier
    
    LLM API call usage metrics including tokens, costs, quotas, and billing information
    
    Turn abort information including the reason for termination
    
    User-initiated tool invocation request with tool name and arguments
    
    Tool execution startup details including MCP server information when applicable
    
    Streaming tool execution output for incremental result display
    
    Tool execution progress notification with status message
    
    Tool execution completion results including success status, detailed output, and error
    information
    
    Skill invocation details including content, allowed tools, and plugin metadata
    
    Sub-agent startup details including parent tool call and agent information
    
    Sub-agent completion details for successful execution
    
    Sub-agent failure details including error message and agent information
    
    Custom agent selection details including name and available tools
    
    Empty payload; the event signals that the custom agent was deselected, returning to the
    default agent
    
    Hook invocation start details including type and input data
    
    Hook invocation completion details including output, success status, and error
    information
    
    System or developer message content with role and optional template metadata
    
    System-generated notification for runtime events like background task completion
    
    Permission request notification requiring client approval with request details
    
    Permission request completion notification signaling UI dismissal
    
    User input request notification with question and optional predefined choices
    
    User input request completion notification signaling UI dismissal
    
    Structured form elicitation request with JSON schema definition for form fields
    
    Elicitation request completion notification signaling UI dismissal
    
    External tool invocation request for client-side tool execution
    
    External tool completion notification signaling UI dismissal
    
    Queued slash command dispatch request for client execution
    
    Queued command completion notification signaling UI dismissal
    
    Plan approval request with plan content and available user actions
    
    Plan mode exit completion notification signaling UI dismissal
    """
    id: UUID
    """Unique event identifier (UUID v4), generated when the event is emitted"""

    timestamp: datetime
    """ISO 8601 timestamp when the event was created"""

    type: SessionEventType
    ephemeral: bool | None = None
    """When true, the event is transient and not persisted to the session event log on disk"""

    parent_id: UUID | None = None
    """ID of the chronologically preceding event in the session, forming a linked chain. Null
    for the first event.
    """

    @staticmethod
    def from_dict(obj: Any) -> 'SessionEvent':
        assert isinstance(obj, dict)
        data = Data.from_dict(obj.get("data"))
        id = UUID(obj.get("id"))
        timestamp = from_datetime(obj.get("timestamp"))
        type = SessionEventType(obj.get("type"))
        ephemeral = from_union([from_bool, from_none], obj.get("ephemeral"))
        parent_id = from_union([from_none, lambda x: UUID(x)], obj.get("parentId"))
        return SessionEvent(data, id, timestamp, type, ephemeral, parent_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = to_class(Data, self.data)
        result["id"] = str(self.id)
        result["timestamp"] = self.timestamp.isoformat()
        result["type"] = to_enum(SessionEventType, self.type)
        if self.ephemeral is not None:
            result["ephemeral"] = from_union([from_bool, from_none], self.ephemeral)
        result["parentId"] = from_union([from_none, lambda x: str(x)], self.parent_id)
        return result


def session_event_from_dict(s: Any) -> SessionEvent:
    return SessionEvent.from_dict(s)


def session_event_to_dict(x: SessionEvent) -> Any:
    return to_class(SessionEvent, x)
