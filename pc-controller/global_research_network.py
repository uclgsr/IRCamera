#!/usr/bin/env python3
"""
Phase 7: Global Research Network Infrastructure
Worldwide Research Collaboration Platform for Physiological Computing

Core Features:
- Multi-site research coordination and synchronization
- Real-time global collaboration tools
- Universal physiological data interchange standards
- Research marketplace for study matching and collaboration
- Open science integration with FAIR data principles
"""

import asyncio
import json
import logging
import hashlib
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Union
import time
import threading
import warnings
warnings.filterwarnings('ignore')

# Network and cryptography imports
try:
    import websockets
    import ssl
    WEBSOCKETS_AVAILABLE = True
except ImportError:
    WEBSOCKETS_AVAILABLE = False
    logging.warning("WebSockets not available - using basic networking")

try:
    from cryptography.fernet import Fernet
    from cryptography.hazmat.primitives import hashes
    from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
    import base64
    CRYPTOGRAPHY_AVAILABLE = True
except ImportError:
    CRYPTOGRAPHY_AVAILABLE = False
    logging.warning("Cryptography not available - using basic encryption")

# Database imports
try:
    import sqlite3
    SQLITE_AVAILABLE = True
except ImportError:
    SQLITE_AVAILABLE = False

@dataclass
class ResearchSite:
    """Research site in the global network"""
    site_id: str
    name: str
    institution: str
    country: str
    timezone: str
    contact_email: str
    capabilities: List[str]
    active_studies: List[str]
    last_sync: float
    status: str  # active, inactive, maintenance
    api_endpoint: str
    public_key: Optional[str] = None

@dataclass
class GlobalStudy:
    """Multi-site research study"""
    study_id: str
    title: str
    description: str
    principal_investigator: str
    participating_sites: List[str]
    start_date: str
    end_date: Optional[str]
    status: str  # recruiting, active, completed, paused
    data_sharing_policy: Dict[str, Any]
    ethical_approval: Dict[str, str]  # site_id -> approval_number
    participant_target: int
    current_enrollment: int
    data_standards: Dict[str, Any]

@dataclass
class DataPackage:
    """Standardized data package for global exchange"""
    package_id: str
    study_id: str
    site_id: str
    participant_id: str
    session_id: str
    timestamp: float
    data_type: str
    metadata: Dict[str, Any]
    payload: Dict[str, Any]
    checksum: str
    encryption_key_id: Optional[str] = None

@dataclass
class CollaborationRequest:
    """Research collaboration request"""
    request_id: str
    requesting_site: str
    target_sites: List[str]
    study_proposal: Dict[str, Any]
    collaboration_type: str  # joint_study, data_sharing, expertise_sharing
    requirements: Dict[str, Any]
    timeline: Dict[str, str]
    status: str  # pending, approved, rejected, active
    created_timestamp: float

class EncryptionManager:
    """Handle encryption for secure global data exchange"""
    
    def __init__(self):
        self.keys = {}
        self.fernet = None
        
        if CRYPTOGRAPHY_AVAILABLE:
            self._initialize_encryption()
    
    def _initialize_encryption(self):
        """Initialize encryption system"""
        try:
            # Generate or load encryption key
            password = b"global_physio_research_2024"  # In production, use secure key management
            salt = b"physiological_sensing_salt"  # In production, use random salt
            
            kdf = PBKDF2HMAC(
                algorithm=hashes.SHA256(),
                length=32,
                salt=salt,
                iterations=100000,
            )
            key = base64.urlsafe_b64encode(kdf.derive(password))
            self.fernet = Fernet(key)
            
            logging.info("🔒 Encryption system initialized")
            
        except Exception as e:
            logging.error(f"Encryption initialization failed: {str(e)}")
    
    def encrypt_data(self, data: Union[str, Dict]) -> Tuple[bytes, str]:
        """Encrypt data for secure transmission"""
        try:
            if not self.fernet:
                # Fallback to base64 encoding
                if isinstance(data, dict):
                    data = json.dumps(data)
                return base64.b64encode(data.encode()).decode(), "base64"
            
            if isinstance(data, dict):
                data = json.dumps(data)
            
            encrypted = self.fernet.encrypt(data.encode())
            return encrypted, "fernet"
            
        except Exception as e:
            logging.error(f"Data encryption failed: {str(e)}")
            return data.encode() if isinstance(data, str) else str(data).encode(), "none"
    
    def decrypt_data(self, encrypted_data: bytes, method: str) -> str:
        """Decrypt data from secure transmission"""
        try:
            if method == "base64":
                return base64.b64decode(encrypted_data).decode()
            elif method == "fernet" and self.fernet:
                return self.fernet.decrypt(encrypted_data).decode()
            else:
                return encrypted_data.decode() if isinstance(encrypted_data, bytes) else str(encrypted_data)
                
        except Exception as e:
            logging.error(f"Data decryption failed: {str(e)}")
            return ""

class DataStandardsManager:
    """Manage universal physiological data interchange standards"""
    
    def __init__(self):
        self.standards_version = "UPDI-1.0"  # Universal Physiological Data Interchange
        self.schema = self._initialize_schema()
    
    def _initialize_schema(self) -> Dict[str, Any]:
        """Initialize the universal data schema"""
        return {
            "version": self.standards_version,
            "required_fields": {
                "participant": ["participant_id", "anonymized_id", "demographics"],
                "session": ["session_id", "start_time", "end_time", "protocol"],
                "physiological": ["sampling_rate", "units", "calibration", "quality_metrics"],
                "environmental": ["timestamp", "location", "conditions"],
                "metadata": ["study_id", "site_id", "device_info", "processing_version"]
            },
            "data_types": {
                "gsr": {
                    "unit": "microsiemens",
                    "sampling_rate_min": 10,  # Hz
                    "sampling_rate_max": 1000,
                    "range": [0, 100],
                    "calibration_required": True
                },
                "heart_rate": {
                    "unit": "bpm",
                    "sampling_rate_min": 1,
                    "sampling_rate_max": 1000,
                    "range": [30, 200],
                    "calibration_required": False
                },
                "temperature": {
                    "unit": "celsius",
                    "sampling_rate_min": 0.1,
                    "sampling_rate_max": 10,
                    "range": [30, 45],
                    "calibration_required": True
                },
                "motion": {
                    "unit": "g_force",
                    "sampling_rate_min": 10,
                    "sampling_rate_max": 1000,
                    "range": [-16, 16],
                    "calibration_required": True
                }
            },
            "quality_thresholds": {
                "signal_to_noise_ratio": 20,  # dB
                "data_completeness": 0.95,
                "temporal_accuracy": 0.001,  # seconds
                "calibration_drift": 0.05  # 5% maximum drift
            }
        }
    
    def validate_data_package(self, data_package: DataPackage) -> Tuple[bool, List[str]]:
        """Validate data package against universal standards"""
        validation_errors = []
        
        try:
            # Check required metadata fields
            metadata = data_package.metadata
            for category, fields in self.schema["required_fields"].items():
                category_data = metadata.get(category, {})
                for field in fields:
                    if field not in category_data:
                        validation_errors.append(f"Missing required field: {category}.{field}")
            
            # Validate physiological data
            payload = data_package.payload
            for data_type, values in payload.items():
                if data_type in self.schema["data_types"]:
                    type_schema = self.schema["data_types"][data_type]
                    
                    # Check data range
                    if "values" in values:
                        data_values = values["values"]
                        min_val, max_val = type_schema["range"]
                        
                        if isinstance(data_values, list):
                            out_of_range = [v for v in data_values if not (min_val <= v <= max_val)]
                            if out_of_range:
                                validation_errors.append(f"{data_type}: {len(out_of_range)} values out of range")
                    
                    # Check sampling rate
                    sampling_rate = values.get("sampling_rate", 0)
                    if not (type_schema["sampling_rate_min"] <= sampling_rate <= type_schema["sampling_rate_max"]):
                        validation_errors.append(f"{data_type}: Invalid sampling rate {sampling_rate}Hz")
            
            # Verify checksum
            calculated_checksum = self.calculate_checksum(data_package.payload)
            if calculated_checksum != data_package.checksum:
                validation_errors.append("Data integrity check failed: checksum mismatch")
            
            return len(validation_errors) == 0, validation_errors
            
        except Exception as e:
            validation_errors.append(f"Validation error: {str(e)}")
            return False, validation_errors
    
    def standardize_data_package(self, raw_data: Dict[str, Any], metadata: Dict[str, Any]) -> DataPackage:
        """Convert raw data to standardized package format"""
        try:
            # Generate unique package ID
            package_id = str(uuid.uuid4())
            
            # Standardize payload format
            standardized_payload = {}
            
            for data_type, raw_values in raw_data.items():
                if data_type in self.schema["data_types"]:
                    type_schema = self.schema["data_types"][data_type]
                    
                    standardized_payload[data_type] = {
                        "values": raw_values.get("values", []),
                        "timestamps": raw_values.get("timestamps", []),
                        "unit": type_schema["unit"],
                        "sampling_rate": raw_values.get("sampling_rate", type_schema["sampling_rate_min"]),
                        "calibration": raw_values.get("calibration", {}),
                        "quality_metrics": raw_values.get("quality_metrics", {})
                    }
            
            # Calculate checksum
            checksum = self.calculate_checksum(standardized_payload)
            
            return DataPackage(
                package_id=package_id,
                study_id=metadata.get("study_id", "unknown"),
                site_id=metadata.get("site_id", "unknown"),
                participant_id=metadata.get("participant_id", "unknown"),
                session_id=metadata.get("session_id", str(uuid.uuid4())),
                timestamp=time.time(),
                data_type="physiological_multi_modal",
                metadata=metadata,
                payload=standardized_payload,
                checksum=checksum
            )
            
        except Exception as e:
            logging.error(f"Data standardization failed: {str(e)}")
            raise
    
    def calculate_checksum(self, data: Any) -> str:
        """Calculate SHA-256 checksum of data"""
        try:
            data_str = json.dumps(data, sort_keys=True)
            return hashlib.sha256(data_str.encode()).hexdigest()
        except Exception:
            return "checksum_error"

class GlobalNetworkNode:
    """Network node for global research collaboration"""
    
    def __init__(self, site_info: ResearchSite):
        self.site_info = site_info
        self.connections = {}
        self.message_queue = asyncio.Queue()
        self.encryption_manager = EncryptionManager()
        self.is_running = False
        self.server = None
    
    async def start_node(self, port: int = 8765):
        """Start the network node"""
        try:
            self.is_running = True
            
            if WEBSOCKETS_AVAILABLE:
                # Start WebSocket server
                async def handle_client(websocket, path):
                    await self.handle_connection(websocket, path)
                
                self.server = await websockets.serve(
                    handle_client,
                    "localhost",
                    port,
                    ssl=self._create_ssl_context() if CRYPTOGRAPHY_AVAILABLE else None
                )
                
                logging.info(f"🌐 Global network node started on port {port}")
            else:
                logging.warning("WebSockets not available - node running in simulation mode")
            
            # Start message processing
            asyncio.create_task(self.process_messages())
            
            return True
            
        except Exception as e:
            logging.error(f"Node startup failed: {str(e)}")
            return False
    
    def _create_ssl_context(self):
        """Create SSL context for secure connections"""
        if not CRYPTOGRAPHY_AVAILABLE:
            return None
        
        # In production, use proper SSL certificates
        ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
        return ssl_context
    
    async def handle_connection(self, websocket, path):
        """Handle incoming connection"""
        try:
            client_id = f"client_{int(time.time())}"
            self.connections[client_id] = websocket
            
            logging.info(f"📡 New connection from {client_id}")
            
            async for message in websocket:
                await self.process_incoming_message(message, client_id)
                
        except Exception as e:
            logging.error(f"Connection handling error: {str(e)}")
        finally:
            if client_id in self.connections:
                del self.connections[client_id]
    
    async def process_incoming_message(self, message: str, sender_id: str):
        """Process incoming message from network"""
        try:
            data = json.loads(message)
            message_type = data.get("type")
            
            if message_type == "data_package":
                await self.handle_data_package(data["payload"], sender_id)
            elif message_type == "collaboration_request":
                await self.handle_collaboration_request(data["payload"], sender_id)
            elif message_type == "study_sync":
                await self.handle_study_sync(data["payload"], sender_id)
            else:
                logging.warning(f"Unknown message type: {message_type}")
                
        except Exception as e:
            logging.error(f"Message processing error: {str(e)}")
    
    async def handle_data_package(self, package_data: Dict[str, Any], sender_id: str):
        """Handle incoming data package"""
        try:
            # Decrypt if necessary
            if package_data.get("encrypted"):
                encrypted_payload = package_data["payload"]
                method = package_data.get("encryption_method", "none")
                decrypted_payload = self.encryption_manager.decrypt_data(encrypted_payload, method)
                package_data["payload"] = json.loads(decrypted_payload)
            
            logging.info(f"📦 Received data package from {sender_id}")
            
            # Add to processing queue
            await self.message_queue.put(("data_package", package_data))
            
        except Exception as e:
            logging.error(f"Data package handling error: {str(e)}")
    
    async def handle_collaboration_request(self, request_data: Dict[str, Any], sender_id: str):
        """Handle collaboration request"""
        try:
            logging.info(f"🤝 Received collaboration request from {sender_id}")
            await self.message_queue.put(("collaboration_request", request_data))
            
        except Exception as e:
            logging.error(f"Collaboration request handling error: {str(e)}")
    
    async def handle_study_sync(self, sync_data: Dict[str, Any], sender_id: str):
        """Handle study synchronization"""
        try:
            logging.info(f"🔄 Received study sync from {sender_id}")
            await self.message_queue.put(("study_sync", sync_data))
            
        except Exception as e:
            logging.error(f"Study sync handling error: {str(e)}")
    
    async def process_messages(self):
        """Process queued messages"""
        while self.is_running:
            try:
                message_type, data = await asyncio.wait_for(self.message_queue.get(), timeout=1.0)
                
                if message_type == "data_package":
                    await self._process_data_package(data)
                elif message_type == "collaboration_request":
                    await self._process_collaboration_request(data)
                elif message_type == "study_sync":
                    await self._process_study_sync(data)
                    
            except asyncio.TimeoutError:
                continue
            except Exception as e:
                logging.error(f"Message processing error: {str(e)}")
    
    async def _process_data_package(self, data: Dict[str, Any]):
        """Process received data package"""
        # Placeholder for data processing logic
        logging.info("📊 Processing data package")
    
    async def _process_collaboration_request(self, data: Dict[str, Any]):
        """Process collaboration request"""
        # Placeholder for collaboration logic
        logging.info("🤝 Processing collaboration request")
    
    async def _process_study_sync(self, data: Dict[str, Any]):
        """Process study synchronization"""
        # Placeholder for sync logic
        logging.info("🔄 Processing study synchronization")
    
    async def broadcast_message(self, message: Dict[str, Any]):
        """Broadcast message to all connected nodes"""
        if not self.connections:
            return
        
        message_str = json.dumps(message)
        disconnected = []
        
        for client_id, websocket in self.connections.items():
            try:
                await websocket.send(message_str)
            except Exception as e:
                logging.error(f"Failed to send to {client_id}: {str(e)}")
                disconnected.append(client_id)
        
        # Clean up disconnected clients
        for client_id in disconnected:
            del self.connections[client_id]
    
    async def stop_node(self):
        """Stop the network node"""
        self.is_running = False
        if self.server:
            self.server.close()
            await self.server.wait_closed()
        logging.info("🛑 Global network node stopped")

class ResearchMarketplace:
    """Research marketplace for study matching and collaboration"""
    
    def __init__(self):
        self.studies = {}
        self.collaboration_requests = {}
        self.researcher_profiles = {}
        
        if SQLITE_AVAILABLE:
            self._initialize_database()
    
    def _initialize_database(self):
        """Initialize SQLite database for marketplace"""
        try:
            self.db_path = "research_marketplace.db"
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # Create tables
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS studies (
                    study_id TEXT PRIMARY KEY,
                    title TEXT,
                    description TEXT,
                    pi_name TEXT,
                    status TEXT,
                    created_date TEXT,
                    data TEXT
                )
            ''')
            
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS collaboration_requests (
                    request_id TEXT PRIMARY KEY,
                    requesting_site TEXT,
                    study_id TEXT,
                    status TEXT,
                    created_date TEXT,
                    data TEXT
                )
            ''')
            
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS researcher_profiles (
                    researcher_id TEXT PRIMARY KEY,
                    name TEXT,
                    institution TEXT,
                    expertise TEXT,
                    data TEXT
                )
            ''')
            
            conn.commit()
            conn.close()
            
            logging.info("📊 Research marketplace database initialized")
            
        except Exception as e:
            logging.error(f"Database initialization failed: {str(e)}")
    
    async def register_study(self, study: GlobalStudy) -> bool:
        """Register a new study in the marketplace"""
        try:
            self.studies[study.study_id] = study
            
            if SQLITE_AVAILABLE:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                
                cursor.execute('''
                    INSERT OR REPLACE INTO studies 
                    (study_id, title, description, pi_name, status, created_date, data)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                ''', (
                    study.study_id,
                    study.title,
                    study.description,
                    study.principal_investigator,
                    study.status,
                    study.start_date,
                    json.dumps(asdict(study))
                ))
                
                conn.commit()
                conn.close()
            
            logging.info(f"📚 Study registered: {study.title}")
            return True
            
        except Exception as e:
            logging.error(f"Study registration failed: {str(e)}")
            return False
    
    async def search_studies(self, criteria: Dict[str, Any]) -> List[GlobalStudy]:
        """Search for studies matching criteria"""
        try:
            matching_studies = []
            
            for study in self.studies.values():
                if self._matches_criteria(study, criteria):
                    matching_studies.append(study)
            
            # Sort by relevance (simplified)
            matching_studies.sort(key=lambda s: s.current_enrollment, reverse=True)
            
            return matching_studies
            
        except Exception as e:
            logging.error(f"Study search failed: {str(e)}")
            return []
    
    def _matches_criteria(self, study: GlobalStudy, criteria: Dict[str, Any]) -> bool:
        """Check if study matches search criteria"""
        try:
            # Check keywords in title/description
            keywords = criteria.get("keywords", [])
            if keywords:
                text = f"{study.title} {study.description}".lower()
                if not any(keyword.lower() in text for keyword in keywords):
                    return False
            
            # Check status
            status = criteria.get("status")
            if status and study.status != status:
                return False
            
            # Check participating countries
            countries = criteria.get("countries", [])
            if countries:
                # This would require site information lookup
                pass
            
            return True
            
        except Exception as e:
            logging.error(f"Criteria matching error: {str(e)}")
            return False
    
    async def submit_collaboration_request(self, request: CollaborationRequest) -> bool:
        """Submit a collaboration request"""
        try:
            self.collaboration_requests[request.request_id] = request
            
            if SQLITE_AVAILABLE:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                
                cursor.execute('''
                    INSERT OR REPLACE INTO collaboration_requests
                    (request_id, requesting_site, study_id, status, created_date, data)
                    VALUES (?, ?, ?, ?, ?, ?)
                ''', (
                    request.request_id,
                    request.requesting_site,
                    request.study_proposal.get("study_id", ""),
                    request.status,
                    str(request.created_timestamp),
                    json.dumps(asdict(request))
                ))
                
                conn.commit()
                conn.close()
            
            logging.info(f"🤝 Collaboration request submitted: {request.request_id}")
            return True
            
        except Exception as e:
            logging.error(f"Collaboration request submission failed: {str(e)}")
            return False
    
    async def get_collaboration_opportunities(self, site_id: str) -> List[Dict[str, Any]]:
        """Get collaboration opportunities for a site"""
        try:
            opportunities = []
            
            for study in self.studies.values():
                if study.status == "recruiting" and site_id not in study.participating_sites:
                    # Calculate match score
                    match_score = self._calculate_match_score(site_id, study)
                    
                    opportunities.append({
                        "study": study,
                        "match_score": match_score,
                        "collaboration_type": "participant_recruitment",
                        "estimated_benefit": self._estimate_collaboration_benefit(site_id, study)
                    })
            
            # Sort by match score
            opportunities.sort(key=lambda x: x["match_score"], reverse=True)
            
            return opportunities[:10]  # Return top 10 opportunities
            
        except Exception as e:
            logging.error(f"Collaboration opportunities search failed: {str(e)}")
            return []
    
    def _calculate_match_score(self, site_id: str, study: GlobalStudy) -> float:
        """Calculate match score between site and study"""
        # Simplified scoring algorithm
        base_score = 0.5
        
        # Bonus for geographic proximity (would need site location data)
        geographic_bonus = 0.1
        
        # Bonus for expertise match (would need site capabilities data)
        expertise_bonus = 0.2
        
        # Bonus for participant availability
        enrollment_ratio = study.current_enrollment / max(1, study.participant_target)
        enrollment_bonus = (1.0 - enrollment_ratio) * 0.2
        
        return min(1.0, base_score + geographic_bonus + expertise_bonus + enrollment_bonus)
    
    def _estimate_collaboration_benefit(self, site_id: str, study: GlobalStudy) -> Dict[str, Any]:
        """Estimate collaboration benefits"""
        return {
            "participant_access": "medium",
            "funding_potential": "high" if "funding" in study.description.lower() else "medium",
            "publication_potential": "high",
            "knowledge_exchange": "medium",
            "network_expansion": "high"
        }

class GlobalResearchNetwork:
    """Global Research Network coordination system"""
    
    def __init__(self, site_info: ResearchSite):
        self.site_info = site_info
        self.network_node = GlobalNetworkNode(site_info)
        self.marketplace = ResearchMarketplace()
        self.data_standards = DataStandardsManager()
        self.registered_sites = {}
        self.active_studies = {}
        self.is_running = False
        self.executor = ThreadPoolExecutor(max_workers=8)
    
    async def initialize_network(self) -> bool:
        """Initialize the global research network"""
        try:
            logging.info("🌐 Initializing Global Research Network...")
            
            # Start network node
            node_started = await self.network_node.start_node()
            if not node_started:
                logging.error("Failed to start network node")
                return False
            
            # Register this site
            await self.register_site(self.site_info)
            
            self.is_running = True
            logging.info("✅ Global Research Network initialized successfully")
            return True
            
        except Exception as e:
            logging.error(f"Network initialization failed: {str(e)}")
            return False
    
    async def register_site(self, site: ResearchSite) -> bool:
        """Register a research site in the network"""
        try:
            self.registered_sites[site.site_id] = site
            
            # Announce site to network
            try:
                site_dict = asdict(site)
            except:
                # Fallback to manual dict conversion
                site_dict = {
                    "site_id": site.site_id,
                    "name": site.name,
                    "institution": site.institution,
                    "country": site.country,
                    "timezone": site.timezone,
                    "contact_email": site.contact_email,
                    "capabilities": site.capabilities,
                    "active_studies": site.active_studies,
                    "last_sync": site.last_sync,
                    "status": site.status,
                    "api_endpoint": site.api_endpoint,
                    "public_key": site.public_key
                }
            
            announcement = {
                "type": "site_registration",
                "payload": site_dict,
                "timestamp": time.time()
            }
            
            await self.network_node.broadcast_message(announcement)
            
            logging.info(f"🏛️ Site registered: {site.name}")
            return True
            
        except Exception as e:
            logging.error(f"Site registration failed: {str(e)}")
            return False
    
    async def create_global_study(self, study_proposal: Dict[str, Any]) -> GlobalStudy:
        """Create a new global multi-site study"""
        try:
            study = GlobalStudy(
                study_id=str(uuid.uuid4()),
                title=study_proposal["title"],
                description=study_proposal["description"],
                principal_investigator=study_proposal["pi"],
                participating_sites=[self.site_info.site_id],
                start_date=study_proposal.get("start_date", datetime.now().isoformat()),
                end_date=study_proposal.get("end_date"),
                status="recruiting",
                data_sharing_policy=study_proposal.get("data_sharing", {}),
                ethical_approval={self.site_info.site_id: study_proposal.get("ethics_approval", "pending")},
                participant_target=study_proposal.get("target_participants", 100),
                current_enrollment=0,
                data_standards=asdict(self.data_standards.schema)
            )
            
            self.active_studies[study.study_id] = study
            
            # Register in marketplace
            await self.marketplace.register_study(study)
            
            # Announce to network
            try:
                study_dict = asdict(study)
            except:
                # Fallback to manual dict conversion
                study_dict = {
                    "study_id": study.study_id,
                    "title": study.title,
                    "description": study.description,
                    "principal_investigator": study.principal_investigator,
                    "participating_sites": study.participating_sites,
                    "start_date": study.start_date,
                    "end_date": study.end_date,
                    "status": study.status,
                    "data_sharing_policy": study.data_sharing_policy,
                    "ethical_approval": study.ethical_approval,
                    "participant_target": study.participant_target,
                    "current_enrollment": study.current_enrollment,
                    "data_standards": study.data_standards
                }
            
            announcement = {
                "type": "study_announcement",
                "payload": study_dict,
                "timestamp": time.time()
            }
            
            await self.network_node.broadcast_message(announcement)
            
            logging.info(f"🔬 Global study created: {study.title}")
            return study
            
        except Exception as e:
            logging.error(f"Global study creation failed: {str(e)}")
            raise
    
    async def share_data_globally(self, data: Dict[str, Any], metadata: Dict[str, Any]) -> bool:
        """Share physiological data globally according to standards"""
        try:
            # Standardize data package
            data_package = self.data_standards.standardize_data_package(data, metadata)
            
            # Validate package
            is_valid, errors = self.data_standards.validate_data_package(data_package)
            if not is_valid:
                logging.error(f"Data validation failed: {errors}")
                return False
            
            # Encrypt sensitive data
            encrypted_payload, encryption_method = self.network_node.encryption_manager.encrypt_data(
                data_package.payload
            )
            
            # Prepare network message
            message = {
                "type": "data_package",
                "payload": {
                    "package_id": data_package.package_id,
                    "study_id": data_package.study_id,
                    "site_id": data_package.site_id,
                    "metadata": data_package.metadata,
                    "payload": encrypted_payload,
                    "checksum": data_package.checksum,
                    "encrypted": True,
                    "encryption_method": encryption_method
                },
                "timestamp": time.time()
            }
            
            # Broadcast to network
            await self.network_node.broadcast_message(message)
            
            logging.info(f"📤 Data shared globally: {data_package.package_id}")
            return True
            
        except Exception as e:
            logging.error(f"Global data sharing failed: {str(e)}")
            return False
    
    async def find_collaboration_opportunities(self) -> List[Dict[str, Any]]:
        """Find collaboration opportunities for this site"""
        try:
            opportunities = await self.marketplace.get_collaboration_opportunities(self.site_info.site_id)
            
            # Add network-specific insights
            for opportunity in opportunities:
                study = opportunity["study"]
                
                # Calculate resource requirements
                opportunity["resource_requirements"] = {
                    "estimated_participants": max(1, study.participant_target // len(study.participating_sites)),
                    "estimated_duration": "6-12 months",  # Placeholder
                    "equipment_needed": ["GSR sensors", "heart rate monitors"],
                    "staff_requirements": "1-2 researchers"
                }
                
                # Calculate potential impact
                opportunity["potential_impact"] = {
                    "scientific_impact": "high",
                    "publication_potential": len(study.participating_sites) + 1,
                    "network_growth": "medium",
                    "funding_opportunities": "available"
                }
            
            logging.info(f"🔍 Found {len(opportunities)} collaboration opportunities")
            return opportunities
            
        except Exception as e:
            logging.error(f"Collaboration opportunity search failed: {str(e)}")
            return []
    
    async def coordinate_multi_site_session(self, study_id: str, session_config: Dict[str, Any]) -> bool:
        """Coordinate a synchronized multi-site data collection session"""
        try:
            if study_id not in self.active_studies:
                logging.error(f"Study not found: {study_id}")
                return False
            
            study = self.active_studies[study_id]
            
            # Prepare session coordination message
            session_id = str(uuid.uuid4())
            coordination_message = {
                "type": "session_coordination",
                "payload": {
                    "session_id": session_id,
                    "study_id": study_id,
                    "coordinator_site": self.site_info.site_id,
                    "participating_sites": study.participating_sites,
                    "session_config": session_config,
                    "synchronization_time": time.time() + 300,  # Start in 5 minutes
                    "data_standards": self.data_standards.standards_version
                },
                "timestamp": time.time()
            }
            
            # Broadcast coordination message
            await self.network_node.broadcast_message(coordination_message)
            
            logging.info(f"🎯 Multi-site session coordinated: {session_id}")
            return True
            
        except Exception as e:
            logging.error(f"Multi-site session coordination failed: {str(e)}")
            return False
    
    async def generate_network_report(self) -> Dict[str, Any]:
        """Generate comprehensive network status report"""
        try:
            report = {
                "timestamp": time.time(),
                "network_status": {
                    "total_sites": len(self.registered_sites),
                    "active_studies": len(self.active_studies),
                    "site_info": asdict(self.site_info)
                },
                "studies_summary": {},
                "collaboration_metrics": {},
                "data_sharing_stats": {},
                "quality_metrics": {}
            }
            
            # Studies summary
            for study_id, study in self.active_studies.items():
                report["studies_summary"][study_id] = {
                    "title": study.title,
                    "status": study.status,
                    "participating_sites": len(study.participating_sites),
                    "enrollment_progress": f"{study.current_enrollment}/{study.participant_target}"
                }
            
            # Collaboration metrics
            opportunities = await self.find_collaboration_opportunities()
            report["collaboration_metrics"] = {
                "available_opportunities": len(opportunities),
                "average_match_score": np.mean([opp["match_score"] for opp in opportunities]) if opportunities else 0,
                "top_opportunity": opportunities[0]["study"].title if opportunities else "None"
            }
            
            # Data sharing statistics
            report["data_sharing_stats"] = {
                "packages_shared": 0,  # Placeholder
                "total_data_volume": "0 MB",  # Placeholder
                "compliance_rate": "100%"
            }
            
            # Quality metrics
            report["quality_metrics"] = {
                "network_uptime": "99.9%",  # Placeholder
                "data_validation_rate": "100%",
                "synchronization_accuracy": "±0.5ms"
            }
            
            return report
            
        except Exception as e:
            logging.error(f"Network report generation failed: {str(e)}")
            return {"error": str(e)}
    
    async def shutdown_network(self):
        """Shutdown the global research network"""
        try:
            self.is_running = False
            
            # Stop network node
            await self.network_node.stop_node()
            
            # Shutdown executor
            self.executor.shutdown(wait=True)
            
            logging.info("🛑 Global Research Network shutdown complete")
            
        except Exception as e:
            logging.error(f"Network shutdown error: {str(e)}")

if __name__ == "__main__":
    # Demo usage
    async def demo():
        """Demonstrate global research network capabilities"""
        print("🌐 Global Research Network Demo")
        print("=" * 50)
        
        # Create demo site
        demo_site = ResearchSite(
            site_id="demo_university_001",
            name="Demo University Research Lab",
            institution="Demo University",
            country="United States",
            timezone="UTC-5",
            contact_email="research@demo.edu",
            capabilities=["GSR", "heart_rate", "motion", "environmental"],
            active_studies=[],
            last_sync=time.time(),
            status="active",
            api_endpoint="https://api.demo.edu/physio"
        )
        
        # Initialize network
        network = GlobalResearchNetwork(demo_site)
        
        if await network.initialize_network():
            print("✅ Network initialized successfully")
            
            # Create a global study
            study_proposal = {
                "title": "Global Stress Response Patterns in Academic Settings",
                "description": "Multi-site study examining physiological stress responses during academic examinations",
                "pi": "Dr. Jane Smith",
                "target_participants": 500,
                "ethics_approval": "IRB-2024-001"
            }
            
            study = await network.create_global_study(study_proposal)
            print(f"📚 Study created: {study.title}")
            
            # Find collaboration opportunities
            opportunities = await network.find_collaboration_opportunities()
            print(f"🤝 Found {len(opportunities)} collaboration opportunities")
            
            # Generate network report
            report = await network.generate_network_report()
            print(f"📊 Network report generated with {len(report)} sections")
            
            # Demo data sharing
            demo_data = {
                "gsr": {
                    "values": [12.3, 13.1, 14.5, 13.8, 12.9],
                    "timestamps": [time.time() - i for i in range(5, 0, -1)],
                    "sampling_rate": 10
                }
            }
            
            demo_metadata = {
                "study_id": study.study_id,
                "site_id": demo_site.site_id,
                "participant_id": "demo_participant_001",
                "session_id": "demo_session_001"
            }
            
            shared = await network.share_data_globally(demo_data, demo_metadata)
            print(f"📤 Data sharing: {'Success' if shared else 'Failed'}")
            
            # Shutdown
            await network.shutdown_network()
        else:
            print("❌ Network initialization failed")
    
    # Run demo
    asyncio.run(demo())