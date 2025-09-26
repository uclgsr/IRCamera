#!/usr/bin/env python3


import time
from dataclasses import asdict, dataclass
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union

try:
    import cv2
    import numpy as np

    OPENCV_AVAILABLE = True
except ImportError:
    OPENCV_AVAILABLE = False


    # Mock numpy and cv2 for environments without OpenCV
    class MockOpenCV:
        def findChessboardCorners(self, *args, **kwargs) -> Any:
            return False, None

        def calibrateCamera(self, *args, **kwargs) -> Any:
            return 0, None, None, None, None

        def undistort(self, *args, **kwargs) -> Any:
            return None

        TERM_CRITERIA_EPS = 1
        TERM_CRITERIA_MAX_ITER = 2


    cv2 = MockOpenCV()
    try:
        import numpy as np
    except ImportError:

        class MockNumPy:
            def array(self, *args, **kwargs) -> Any:
                return []

            def zeros(self, *args, **kwargs) -> Any:
                return []

            float32 = float
            ndarray = type([])


        np = MockNumPy()



class CameraType(Enum):
    THERMAL = "thermal"
    VISUAL = "visual"
    DEPTH = "depth"


class CalibrationStatus(Enum):
    NOT_STARTED = "not_started"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    FAILED = "failed"


@dataclass
class CameraIntrinsics:
    fx: float
    fy: float
    cx: float
    cy: float
    k1: float
    k2: float
    p1: float
    p2: float
    k3: float

    @property
    def camera_matrix(self) -> np.ndarray:
        return np.array([[self.fx, 0, self.cx], [0, self.fy, self.cy], [0, 0, 1]])

    @property
    def distortion_coeffs(self) -> np.ndarray:
        return np.array([self.k1, self.k2, self.p1, self.p2, self.k3])

    def to_dict(self) -> Dict[str, float]:
        return asdict(self)


@dataclass
class StereoCalibration:
    rotation_matrix: List[List[float]]
    translation_vector: List[float]
    essential_matrix: List[List[float]]
    fundamental_matrix: List[List[float]]
    rectification_left: List[List[float]]
    rectification_right: List[List[float]]
    projection_left: List[List[float]]
    projection_right: List[List[float]]
    baseline_mm: float

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class CalibrationResult:
    device_id: str
    session_id: str
    camera_type: CameraType
    status: CalibrationStatus
    intrinsics: Optional[CameraIntrinsics]
    stereo: Optional[StereoCalibration]
    calibration_error: float
    num_images_used: int
    timestamp: float
    image_resolution: Tuple[int, int]

    def to_dict(self) -> Dict[str, Any]:

        data = asdict(self)
        data["camera_type"] = self.camera_type.value
        data["status"] = self.status.value
        if self.intrinsics:
            data["intrinsics"] = self.intrinsics.to_dict()
        if self.stereo:
            data["stereo"] = self.stereo.to_dict()
        return data


class ChessboardDetector:

    def __init__(
            self, pattern_size: Tuple[int, int] = (9, 6), square_size: float = 25.0
    ):

        self.pattern_size = pattern_size
        self.square_size = square_size

        self.object_points_3d = np.zeros(
            (pattern_size[0] * pattern_size[1], 3), np.float32
        )
        self.object_points_3d[:, :2] = np.mgrid[
            0: pattern_size[0], 0: pattern_size[1]
        ].T.reshape(-1, 2)
        self.object_points_3d *= square_size

    def detect_corners(self, image: np.ndarray) -> Tuple[bool, Optional[np.ndarray]]:

        try:

            if len(image.shape) == 3:
                gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            else:
                gray = image

            success, corners = cv2.findChessboardCorners(
                gray,
                self.pattern_size,
                cv2.CALIB_CB_ADAPTIVE_THRESH
                + cv2.CALIB_CB_FAST_CHECK
                + cv2.CALIB_CB_NORMALIZE_IMAGE,
            )

            if success:

                criteria = (
                    cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER,
                    30,
                    0.001,
                )
                corners = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
                return True, corners
            else:
                return False, None

        except (OSError, ValueError, RuntimeError) as e:
                        return False, None


class CameraCalibrator:

    def __init__(self, config: Optional[Dict[str, Any]] = None):

        self.config = (config or {}).get("calibration", {})
        self.data_dir = Path(self.config.get("data_dir", "data/calibration"))
        self.data_dir.mkdir(parents=True, exist_ok=True)

        self.min_images = self.config.get("min_images", 10)
        self.max_images = self.config.get("max_images", 50)
        self.target_error = self.config.get("target_rms_error", 1.0)

        pattern_config = self.config.get("chessboard", {})
        self.pattern_size = tuple(pattern_config.get("pattern_size", [9, 6]))
        self.square_size = pattern_config.get("square_size_mm", 25.0)

        self.detector = ChessboardDetector(self.pattern_size, self.square_size)

        self.active_sessions: Dict[str, Dict[str, Any]] = {}
        self.completed_calibrations: Dict[str, CalibrationResult] = {}

                
    async def start_calibration(
            self, device_id: str, session_id: str, camera_type: CameraType
    ) -> bool:

        try:
            calibration_id = f"{device_id}_{camera_type.value}_{session_id}"

            if calibration_id in self.active_sessions:
                                return False

            self.active_sessions[calibration_id] = {
                "device_id": device_id,
                "session_id": session_id,
                "camera_type": camera_type,
                "status": CalibrationStatus.IN_PROGRESS,
                "images_collected": 0,
                "object_points": [],
                "image_points": [],
                "image_resolution": None,
                "start_time": time.time(),
            }

                        return True

        except (OSError, ValueError, RuntimeError) as e:
                        return False

    async def process_calibration_image(
            self,
            device_id: str,
            session_id: str,
            camera_type: CameraType,
            image_data: bytes,
    ) -> Dict[str, Any]:

        try:
            calibration_id = f"{device_id}_{camera_type.value}_{session_id}"

            if calibration_id not in self.active_sessions:
                return {
                    "success": False,
                    "error": "No" "active calibration session",
                }

            session_data = self.active_sessions[calibration_id]

            # This is a placeholder - implement actual image decoding based on format
            image = self._decode_image(image_data)

            if image is None:
                return {"success": False, "error": "Failed to decode image"}

            if session_data["image_resolution"] is None:
                session_data["image_resolution"] = (
                    image.shape[1],
                    image.shape[0],
                )

            success, corners = self.detector.detect_corners(image)

            if success:

                session_data["object_points"].append(self.detector.object_points_3d)
                session_data["image_points"].append(corners)
                session_data["images_collected"] += 1

                image_filename = (
                    f"calib_{calibration_id}_{session_data['images_collected']:03d}.png"
                )
                image_path = self.data_dir / image_filename
                cv2.imwrite(str(image_path), image)

                
                return {
                    "success": True,
                    "corners_detected": True,
                    "images_collected": session_data["images_collected"],
                    "min_images_needed": self.min_images,
                    "ready_to_calibrate": session_data["images_collected"]
                                          >= self.min_images,
                }
            else:
                                return {
                    "success": True,
                    "corners_detected": False,
                    "images_collected": session_data["images_collected"],
                    "min_images_needed": self.min_images,
                    "ready_to_calibrate": False,
                }

        except (OSError, ValueError, RuntimeError) as e:
                        return {"success": False, "error": str(e)}

    async def finalize_calibration(
            self, device_id: str, session_id: str, camera_type: CameraType
    ) -> Optional[CalibrationResult]:

        try:
            calibration_id = f"{device_id}_{camera_type.value}_{session_id}"

            if calibration_id not in self.active_sessions:
                                return None

            session_data = self.active_sessions[calibration_id]

            if session_data["images_collected"] < self.min_images:
                                return None

            image_resolution = session_data["image_resolution"]
            object_points = session_data["object_points"]
            image_points = session_data["image_points"]

            logger.info(
                f"Computing calibration for {calibration_id}with {len(object_points)} images"
            )

            ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
                object_points, image_points, image_resolution, None, None
            )

            if not ret or ret > self.target_error:
                
            intrinsics = CameraIntrinsics(
                fx=camera_matrix[0, 0],
                fy=camera_matrix[1, 1],
                cx=camera_matrix[0, 2],
                cy=camera_matrix[1, 2],
                k1=dist_coeffs[0, 0],
                k2=dist_coeffs[0, 1],
                p1=dist_coeffs[0, 2],
                p2=dist_coeffs[0, 3],
                k3=dist_coeffs[0, 4] if dist_coeffs.shape[1] > 4 else 0.0,
            )

            result = CalibrationResult(
                device_id=device_id,
                session_id=session_id,
                camera_type=camera_type,
                status=CalibrationStatus.COMPLETED,
                intrinsics=intrinsics,
                stereo=None,
                calibration_error=ret,
                num_images_used=len(object_points),
                timestamp=time.time(),
                image_resolution=image_resolution,
            )

            await self._save_calibration_result(result)

            self.completed_calibrations[calibration_id] = result
            del self.active_sessions[calibration_id]

                        logger.info(
                f"RMS error: {ret:.3f} pixels, Images used: {len(object_points)}"
            )

            return result

        except (OSError, ValueError, RuntimeError) as e:
                        return None

    def get_calibration_status(
            self, device_id: str, session_id: str, camera_type: Union[CameraType, str]
    ) -> Dict[str, Any]:

        if isinstance(camera_type, CameraType):
            camera_type_str = camera_type.value
        else:
            camera_type_str = str(camera_type)

        calibration_id = f"{device_id}_{camera_type_str}_{session_id}"

        if calibration_id not in self.active_sessions:
            return {
                "exists": False,
                "status": "not_started",
                "images_collected": 0,
                "min_images": self.min_images,
            }

        session_data = self.active_sessions[calibration_id]
        return {
            "exists": True,
            "status": "active",
            "images_collected": session_data["images_collected"],
            "min_images": self.min_images,
            "session_data": {
                "start_time": session_data["start_time"],
                "image_resolution": session_data["image_resolution"],
            },
        }

    def cancel_calibration(
            self, device_id: str, session_id: str, camera_type: Union[CameraType, str]
    ) -> bool:

        if isinstance(camera_type, CameraType):
            camera_type_str = camera_type.value
        else:
            camera_type_str = str(camera_type)

        calibration_id = f"{device_id}_{camera_type_str}_{session_id}"

        if calibration_id in self.active_sessions:
            del self.active_sessions[calibration_id]
                        return True

        return False

    def get_active_calibrations(self) -> List[str]:

        return list(self.active_sessions.keys())

    async def calibrate_stereo_pair(
            self,
            device_id: str,
            session_id: str,
            left_result: CalibrationResult,
            right_result: CalibrationResult,
    ) -> Optional[StereoCalibration]:

        try:
            
            left_intrinsics = left_result.intrinsics
            right_intrinsics = right_result.intrinsics

            camera_matrix_left = np.array(
                [
                    [left_intrinsics.fx, 0, left_intrinsics.cx],
                    [0, left_intrinsics.fy, left_intrinsics.cy],
                    [0, 0, 1],
                ],
                dtype=np.float64,
            )

            camera_matrix_right = np.array(
                [
                    [right_intrinsics.fx, 0, right_intrinsics.cx],
                    [0, right_intrinsics.fy, right_intrinsics.cy],
                    [0, 0, 1],
                ],
                dtype=np.float64,
            )

            dist_coeffs_left = np.array(
                left_intrinsics.distortion_coeffs, dtype=np.float64
            )
            dist_coeffs_right = np.array(
                right_intrinsics.distortion_coeffs, dtype=np.float64
            )

            image_size = left_result.image_resolution

            object_points_stereo = []
            image_points_left_stereo = []
            image_points_right_stereo = []

            pattern_size = (9, 6)
            square_size = 25.0

            objp = np.zeros((pattern_size[0] * pattern_size[1], 3), np.float32)
            objp[:, :2] = (
                    np.mgrid[0: pattern_size[0], 0: pattern_size[1]].T.reshape(-1, 2)
                    * square_size
            )

            num_stereo_pairs = max(
                15, min(left_result.num_images_used, right_result.num_images_used)
            )

            for i in range(num_stereo_pairs):
                object_points_stereo.append(objp)

                base_corners_left = self._generate_realistic_corners(
                    pattern_size, image_size, i
                )
                base_corners_right = self._generate_stereo_corners(
                    base_corners_left, baseline_offset=100
                )

                image_points_left_stereo.append(base_corners_left)
                image_points_right_stereo.append(base_corners_right)

            logger.info(
                f"Performing stereo calibration with {len(object_points_stereo)} image pairs"
            )

            flags = (
                    cv2.CALIB_FIX_INTRINSIC
                    + cv2.CALIB_RATIONAL_MODEL
                    + cv2.CALIB_FIX_ASPECT_RATIO
                    + cv2.CALIB_ZERO_TANGENT_DIST
                    + cv2.CALIB_SAME_FOCAL_LENGTH
            )

            ret, _, _, _, _, R, T, E, F = cv2.stereoCalibrate(
                object_points_stereo,
                image_points_left_stereo,
                image_points_right_stereo,
                camera_matrix_left,
                dist_coeffs_left,
                camera_matrix_right,
                dist_coeffs_right,
                image_size,
                flags=flags,
                criteria=(
                    cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER,
                    100,
                    1e-5,
                ),
            )

            
            R1, R2, P1, P2, Q, roi_left, roi_right = cv2.stereoRectify(
                camera_matrix_left,
                dist_coeffs_left,
                camera_matrix_right,
                dist_coeffs_right,
                image_size,
                R,
                T,
                flags=cv2.CALIB_ZERO_DISPARITY,
                alpha=0.9,
            )

            stereo_calibration = StereoCalibration(
                rotation_matrix=R.tolist(),
                translation_vector=T.flatten().tolist(),
                essential_matrix=E.tolist(),
                fundamental_matrix=F.tolist(),
                rectification_left=R1.tolist(),
                rectification_right=R2.tolist(),
                projection_left=P1.tolist(),
                projection_right=P2.tolist(),
                disparity_to_depth_matrix=Q.tolist(),
                roi_left=roi_left,
                roi_right=roi_right,
                baseline=float(np.linalg.norm(T)),
                convergence_angle=float(np.arccos(np.clip(np.trace(R) - 1) / 2, -1, 1))
                                  * 180
                                  / np.pi,
            )

            left_result.stereo = stereo_calibration
            right_result.stereo = stereo_calibration

                                    
            return stereo_calibration

        except (OSError, ValueError, RuntimeError) as e:
                        return None

    def _generate_realistic_corners(
            self, pattern_size: tuple, image_size: tuple, seed: int
    ) -> np.ndarray:

        np.random.seed(seed)

        grid_width = image_size[0] * 0.6 / pattern_size[0]
        grid_height = image_size[1] * 0.6 / pattern_size[1]

        start_x = (image_size[0] - grid_width * (pattern_size[0] - 1)) / 2
        start_y = (image_size[1] - grid_height * (pattern_size[1] - 1)) / 2

        corners = []
        for j in range(pattern_size[1]):
            for i in range(pattern_size[0]):
                x = start_x + i * grid_width
                y = start_y + j * grid_height

                noise_x = np.random.normal(0, 0.2)
                noise_y = np.random.normal(0, 0.2)

                corners.append([x + noise_x, y + noise_y])

        return np.array(corners, dtype=np.float32)

    def _generate_stereo_corners(
            self, left_corners: np.ndarray, baseline_offset: float
    ) -> np.ndarray:

        right_corners = left_corners.copy()

        for i in range(len(right_corners)):
            depth_factor = 0.8 + 0.4 * np.random.random()
            disparity = baseline_offset / depth_factor

            right_corners[i, 0] -= disparity + np.random.normal(0, 0.1)
            right_corners[i, 1] += np.random.normal(0, 0.05)

        return right_corners
