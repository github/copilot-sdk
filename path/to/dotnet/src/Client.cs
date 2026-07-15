# complete code
import os
from typing import Dict

class CopilotClientOptions:
    def __init__(self, 
                 environment: Dict[str, str] = None, 
                 telemetry: Dict[str, str] = None, 
                 github_token: str = None, 
                 base_directory: str = None, 
                 mode: str = None, 
                 connection_token: str = None):
        self.environment = environment
        self.telemetry = telemetry
        self.github_token = github_token
        self.base_directory = base_directory
        self.mode = mode
        self.connection_token = connection_token

        self._set_environment_variables()

    def _set_environment_variables(self):
        if self.environment:
            for key, value in self.environment.items():
                os.environ[key] = value

        if self.telemetry:
            for key, value in self.telemetry.items():
                os.environ[key] = value

        if self.github_token:
            os.environ['COPILOT_SDK_AUTH_TOKEN'] = self.github_token

        if self.base_directory:
            os.environ['COPILOT_HOME'] = self.base_directory

        if self.mode == 'Empty':
            os.environ['COPILOT_DISABLE_KEYTAR'] = '1'