import unittest

from protocol_adapter import ProtocolAdapter


class ProtocolAdapterUnitTest(unittest.TestCase):
    def setUp(self) -> None:
        self.adapter = ProtocolAdapter()

    def test_parse_android_message_with_arrays(self) -> None:
        msg = 'HELLO device_id=device_1 sensors=[GSR,RGB] mode="test mode"'
        parsed = self.adapter.parse_android_message(msg)
        self.assertIsNotNone(parsed)
        assert parsed  # for type checker
        self.assertEqual(parsed.type, "HELLO")
        self.assertEqual(parsed.parameters["device_id"], "device_1")
        self.assertEqual(parsed.parameters["sensors"], "[GSR,RGB]")
        self.assertEqual(parsed.parameters["mode"], "test mode")

    def test_android_to_json_invalid_message(self) -> None:
        result = self.adapter.android_to_json("   ")
        self.assertIsNone(result)
        self.assertEqual(self.adapter.parse_errors, 0)

        result = self.adapter.android_to_json("INVALID payload")
        self.assertIsNotNone(result)
        assert result is not None
        self.assertEqual(result["type"], "INVALID")
        self.assertEqual(self.adapter.parse_errors, 0)

    def test_json_to_android_round_trip(self) -> None:
        message = {"type": "START_RECORD", "session_id": "abc", "timestamp": 123}
        legacy = self.adapter.json_to_android(message)
        self.assertIn("START_RECORD", legacy)
        self.assertIn("session_id=abc", legacy)

    def test_helper_creators(self) -> None:
        sync_result = self.adapter.create_sync_result(1, 2, 3, 4, 5)
        self.assertEqual(sync_result, "SYNC_RESULT t1=1 t2=2 t3=3 offset=4 rtt=5")

        ack = self.adapter.create_ack("START_RECORD", session_id="s1")
        self.assertEqual(ack, "ACK cmd=START_RECORD session_id=s1")

        error = self.adapter.create_error("START_RECORD", "FAIL", "bad sensor")
        self.assertEqual(error, 'ERROR cmd=START_RECORD code=FAIL msg="bad sensor"')


if __name__ == "__main__":
    unittest.main()
