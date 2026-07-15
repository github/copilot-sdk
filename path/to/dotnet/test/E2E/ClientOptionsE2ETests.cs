# complete code
import unittest
from unittest.mock import patch
from dotnet.src.Client import CopilotClientOptions

class TestClientOptions(unittest.TestCase):
    def test_set_environment_variables(self):
        options = CopilotClientOptions(
            environment={'TEST_VAR': 'test_value'},
            telemetry={'COPILOT_OTEL_ENABLED': 'true'},
            github_token='test_token',
            base_directory='/test/dir',
            mode='Empty'
        )

        self.assertEqual(os.environ['TEST_VAR'], 'test_value')
        self.assertEqual(os.environ['COPILOT_OTEL_ENABLED'], 'true')
        self.assertEqual(os.environ['COPILOT_SDK_AUTH_TOKEN'], 'test_token')
        self.assertEqual(os.environ['COPILOT_HOME'], '/test/dir')
        self.assertEqual(os.environ['COPILOT_DISABLE_KEYTAR'], '1')

    @patch.dict('os.environ', {})
    def test_set_environment_variables_empty(self):
        options = CopilotClientOptions()

        self.assertEqual(os.environ, {})