# IRCamera Platform - Advanced Enterprise Integration Guide

## 🎯 Overview

This **comprehensive enterprise integration guide** provides detailed strategies, patterns, and
implementations for integrating the IRCamera thermal imaging platform into large-scale enterprise
environments, including advanced cloud infrastructure, microservices architectures, enterprise data
pipelines, AI/ML integration, real-time processing, and mission-critical deployment patterns.

## 📋 Table of Contents

1. [🏗️ Enterprise Architecture Patterns](#enterprise-architecture-patterns) - Advanced enterprise
   design patterns
2. [☁️ Cloud Integration Strategies](#cloud-integration-strategies) - AWS, Azure, GCP deployment
   patterns
3. [🔧 Microservices Integration](#microservices-integration) - Container-based microservices
   architecture
4. [📊 Enterprise Data Pipeline](#enterprise-data-pipeline) - Big data and real-time processing
   pipelines
5. [🛡️ Security & Compliance](#security--compliance) - Enterprise security and regulatory compliance
6. [📈 Monitoring & Observability](#monitoring--observability) - Enterprise monitoring and alerting
7. [⚡ Scalability & Performance](#scalability--performance) - Auto-scaling and performance
   optimization
8. [🔄 Disaster Recovery](#disaster-recovery) - Business continuity and disaster recovery
9. [🤖 AI/ML Enterprise Integration](#ai-ml-enterprise-integration) - Machine learning at enterprise
   scale
10. [📡 Real-Time Processing Integration](#real-time-processing-integration) - Stream processing and
    edge computing
11. [🏢 Enterprise Service Integration](#enterprise-service-integration) - ERP, CRM, and business
    system integration
12. [📊 Enterprise Analytics & BI](#enterprise-analytics--bi) - Business intelligence and advanced
    analytics

---

## 🏗️ Enterprise Architecture Patterns

### 🏢 Advanced Hub-and-Spoke Enterprise Architecture

```mermaid
graph TB
    subgraph "🏢 Enterprise Data Center"
        ESB[Enterprise Service Bus<br/>Apache Kafka + Event Mesh]
        DB[(Enterprise Database<br/>PostgreSQL + MongoDB Cluster)]
        AuthService[Enterprise Authentication<br/>LDAP + SSO + MFA]
        MonitoringStack[Enterprise Monitoring<br/>Prometheus + Grafana + ELK]
        MLPlatform[ML Platform<br/>MLflow + Kubeflow]
        DataLake[Enterprise Data Lake<br/>Hadoop + Spark]
    end
    
    subgraph "☁️ Multi-Cloud Infrastructure"
        AWSCluster[AWS Enterprise<br/>EKS + S3 + Lambda]
        AzureCluster[Azure Enterprise<br/>AKS + Blob + Functions]
        GCPCluster[GCP Enterprise<br/>GKE + Storage + Cloud Functions]
        HybridCloud[Hybrid Cloud<br/>Cross-Cloud Orchestration]
    end
    
    subgraph "🔧 Microservices Ecosystem"
        APIGateway[API Gateway<br/>Kong + Ambassador]
        ServiceMesh[Service Mesh<br/>Istio + Envoy]
        ConfigService[Configuration Service<br/>Consul + Vault]
        DiscoveryService[Service Discovery<br/>Eureka + Consul]
    end
    
    subgraph "📱 Edge Computing Layer"
        EdgeNodes[Edge Computing Nodes<br/>K3s + NVIDIA Jetson]
        IoTGateway[IoT Gateway<br/>AWS IoT + Azure IoT]
        EdgeML[Edge ML Processing<br/>TensorFlow Lite + ONNX]
        5GNetwork[5G/WiFi6 Networks<br/>Ultra-Low Latency]
    end
    end
    
    subgraph "IRCamera Hub Cluster"
        LB[Load Balancer]
        Hub1[PC Controller Hub 1]
        Hub2[PC Controller Hub 2]
        Hub3[PC Controller Hub 3]
        SharedStorage[(Shared Storage)]
    end
    
    subgraph "Field Deployment"
        Android1[Android Device 1]
        Android2[Android Device 2]
        Android3[Android Device 3]
        TC001_1[TC001 Camera 1]
        TC001_2[TC001 Camera 2]
        GSR1[GSR Sensor 1]
        GSR2[GSR Sensor 2]
    end
    
    subgraph "Cloud Integration"
        CloudGateway[API Gateway]
        ProcessingCluster[Processing Cluster]
        DataLake[(Data Lake)]
        ML[ML Pipeline]
    end
    
    ESB --> LB
    LB --> Hub1
    LB --> Hub2
    LB --> Hub3
    
    Hub1 --> SharedStorage
    Hub2 --> SharedStorage
    Hub3 --> SharedStorage
    
    Android1 --> Hub1
    Android2 --> Hub2
    Android3 --> Hub3
    
    TC001_1 --> Android1
    TC001_2 --> Android2
    GSR1 --> Android1
    GSR2 --> Android2
    
    Hub1 --> CloudGateway
    Hub2 --> CloudGateway
    Hub3 --> CloudGateway
    
    CloudGateway --> ProcessingCluster
    ProcessingCluster --> DataLake
    DataLake --> ML
```

### Enterprise Integration Patterns

```python
# Enterprise Integration Hub
from typing import List, Dict, Any, Optional
import asyncio
import aiohttp
from dataclasses import dataclass
from abc import ABC, abstractmethod

@dataclass
class EnterpriseMessage:
    """Standard enterprise message format"""
    message_id: str
    source_system: str
    target_system: str
    message_type: str
    payload: Dict[Any, Any]
    timestamp: float
    priority: int = 5
    retry_count: int = 0
    correlation_id: Optional[str] = None

class EnterpriseIntegrationPattern(ABC):
    """Base class for enterprise integration patterns"""
    
    @abstractmethod
    async def process_message(self, message: EnterpriseMessage) -> bool:
        pass
    
    @abstractmethod
    async def handle_error(self, message: EnterpriseMessage, error: Exception) -> bool:
        pass

class RequestReplyPattern(EnterpriseIntegrationPattern):
    """Request-Reply pattern for synchronous communication"""
    
    def __init__(self, timeout: int = 30):
        self.timeout = timeout
        self.pending_requests = {}
    
    async def send_request(self, request: EnterpriseMessage) -> EnterpriseMessage:
        """Send request and wait for reply"""
        correlation_id = f"{request.message_id}_{int(time.time())}"
        request.correlation_id = correlation_id
        
        # Store request for correlation
        future = asyncio.Future()
        self.pending_requests[correlation_id] = future
        
        try:
            # Send request to target system
            await self.dispatch_message(request)
            
            # Wait for reply with timeout
            reply = await asyncio.wait_for(future, timeout=self.timeout)
            return reply
            
        except asyncio.TimeoutError:
            # Clean up and raise timeout
            self.pending_requests.pop(correlation_id, None)
            raise TimeoutError(f"Request {request.message_id} timed out")
        
        finally:
            self.pending_requests.pop(correlation_id, None)
    
    async def handle_reply(self, reply: EnterpriseMessage):
        """Handle incoming reply message"""
        correlation_id = reply.correlation_id
        
        if correlation_id in self.pending_requests:
            future = self.pending_requests[correlation_id]
            if not future.done():
                future.set_result(reply)

class PublishSubscribePattern(EnterpriseIntegrationPattern):
    """Publish-Subscribe pattern for event-driven communication"""
    
    def __init__(self):
        self.subscribers = {}
        self.message_queue = asyncio.Queue()
    
    def subscribe(self, topic: str, handler: callable):
        """Subscribe to a topic"""
        if topic not in self.subscribers:
            self.subscribers[topic] = []
        self.subscribers[topic].append(handler)
    
    async def publish(self, topic: str, message: EnterpriseMessage):
        """Publish message to topic"""
        message.target_system = topic
        await self.message_queue.put((topic, message))
    
    async def process_messages(self):
        """Process published messages"""
        while True:
            try:
                topic, message = await self.message_queue.get()
                
                if topic in self.subscribers:
                    # Notify all subscribers concurrently
                    tasks = [
                        handler(message) 
                        for handler in self.subscribers[topic]
                    ]
                    await asyncio.gather(*tasks, return_exceptions=True)
                
            except Exception as e:
                logger.error(f"Error processing message: {e}")

class MessageRoutingPattern(EnterpriseIntegrationPattern):
    """Content-based message routing pattern"""
    
    def __init__(self):
        self.routing_rules = []
    
    def add_routing_rule(self, condition: callable, target: str):
        """Add routing rule"""
        self.routing_rules.append((condition, target))
    
    async def process_message(self, message: EnterpriseMessage) -> bool:
        """Route message based on content"""
        for condition, target in self.routing_rules:
            if condition(message):
                routed_message = message.__class__(
                    message_id=f"{message.message_id}_routed",
                    source_system=message.source_system,
                    target_system=target,
                    message_type=message.message_type,
                    payload=message.payload,
                    timestamp=time.time(),
                    correlation_id=message.correlation_id
                )
                
                await self.dispatch_to_target(routed_message, target)
                return True
        
        # No routing rule matched
        await self.handle_unroutable_message(message)
        return False
    
    async def dispatch_to_target(self, message: EnterpriseMessage, target: str):
        """Dispatch message to target system"""
        # Implementation depends on target system type
        if target.startswith("http"):
            await self.send_http_message(message, target)
        elif target.startswith("mq"):
            await self.send_queue_message(message, target)
        else:
            await self.send_internal_message(message, target)
```

---

## Cloud Integration Strategies

### AWS Enterprise Integration

```python
# AWS Enterprise Cloud Integration
import boto3
import json
from botocore.exceptions import ClientError, BotoCoreError
from typing import Dict, List, Any, Optional

class AWSEnterpriseIntegration:
    """Enterprise-grade AWS integration for IRCamera platform"""
    
    def __init__(self, aws_config: Dict[str, Any]):
        self.region = aws_config.get('region', 'us-east-1')
        
        # Initialize AWS clients
        self.s3 = boto3.client('s3', region_name=self.region)
        self.lambda_client = boto3.client('lambda', region_name=self.region)
        self.sqs = boto3.client('sqs', region_name=self.region)
        self.sns = boto3.client('sns', region_name=self.region)
        self.kinesis = boto3.client('kinesis', region_name=self.region)
        self.dynamodb = boto3.resource('dynamodb', region_name=self.region)
        
        # Configuration
        self.bucket_name = aws_config['s3_bucket']
        self.processing_queue = aws_config['sqs_queue']
        self.notification_topic = aws_config['sns_topic']
        self.data_stream = aws_config['kinesis_stream']
    
    async def upload_thermal_session_enterprise(
        self, 
        session_data: ThermalSessionData
    ) -> EnterpriseUploadResult:
        """Enterprise-grade thermal session upload with full compliance"""
        
        try:
            # Step 1: Validate session data
            validation_result = await self.validate_session_data(session_data)
            if not validation_result.is_valid:
                return EnterpriseUploadResult.validation_failed(validation_result.errors)
            
            # Step 2: Encrypt sensitive data
            encrypted_session = await self.encrypt_session_data(session_data)
            
            # Step 3: Upload to S3 with enterprise metadata
            s3_result = await self.upload_to_s3_enterprise(encrypted_session)
            
            # Step 4: Record in audit trail
            audit_entry = await self.create_audit_entry(session_data, s3_result)
            await self.store_audit_entry(audit_entry)
            
            # Step 5: Trigger processing pipeline
            processing_job = await self.trigger_enterprise_processing(s3_result)
            
            # Step 6: Send notifications
            await self.send_enterprise_notifications(session_data, s3_result, processing_job)
            
            return EnterpriseUploadResult(
                session_id=session_data.session_id,
                s3_locations=s3_result.locations,
                processing_job_id=processing_job.job_id,
                audit_entry_id=audit_entry.id,
                compliance_status='COMPLIANT'
            )
            
        except Exception as e:
            # Log error and create incident
            await self.log_enterprise_error(session_data.session_id, e)
            await self.create_incident(session_data.session_id, e)
            
            return EnterpriseUploadResult.error(str(e))
    
    async def upload_to_s3_enterprise(
        self, 
        encrypted_session: EncryptedSessionData
    ) -> S3UploadResult:
        """Upload to S3 with enterprise-grade features"""
        
        locations = {}
        
        try:
            # Upload thermal frames with server-side encryption
            for i, frame in enumerate(encrypted_session.thermal_frames):
                key = f"thermal-data/{encrypted_session.session_id}/frames/frame_{i:06d}.enc"
                
                self.s3.put_object(
                    Bucket=self.bucket_name,
                    Key=key,
                    Body=frame.encrypted_data,
                    ServerSideEncryption='aws:kms',
                    SSEKMSKeyId=encrypted_session.kms_key_id,
                    Metadata={
                        'session-id': encrypted_session.session_id,
                        'frame-index': str(i),
                        'timestamp': str(frame.timestamp),
                        'device-type': encrypted_session.device_type,
                        'data-classification': 'SENSITIVE',
                        'retention-policy': '7-years'
                    },
                    Tagging='Project=IRCamera&Environment=Production&DataType=ThermalFrame'
                )
                
                locations[f'frame_{i}'] = f's3://{self.bucket_name}/{key}'
            
            # Upload session metadata
            metadata_key = f"thermal-data/{encrypted_session.session_id}/metadata.json"
            metadata_json = json.dumps(encrypted_session.metadata, indent=2)
            
            self.s3.put_object(
                Bucket=self.bucket_name,
                Key=metadata_key,
                Body=metadata_json,
                ContentType='application/json',
                ServerSideEncryption='aws:kms',
                SSEKMSKeyId=encrypted_session.kms_key_id,
                Metadata={
                    'session-id': encrypted_session.session_id,
                    'data-type': 'session-metadata',
                    'data-classification': 'SENSITIVE'
                }
            )
            
            locations['metadata'] = f's3://{self.bucket_name}/{metadata_key}'
            
            return S3UploadResult(
                success=True,
                locations=locations,
                encryption_key_id=encrypted_session.kms_key_id
            )
            
        except ClientError as e:
            return S3UploadResult(
                success=False,
                error=f"S3 upload failed: {e}",
                locations=locations
            )
    
    async def trigger_enterprise_processing(
        self, 
        s3_result: S3UploadResult
    ) -> ProcessingJobResult:
        """Trigger enterprise processing pipeline"""
        
        # Create processing job message
        job_message = {
            'job_id': generate_job_id(),
            'session_id': s3_result.session_id,
            's3_locations': s3_result.locations,
            'processing_config': {
                'thermal_analysis': True,
                'quality_assessment': True,
                'anomaly_detection': True,
                'compliance_validation': True
            },
            'priority': 'HIGH',
            'sla_deadline': (datetime.now() + timedelta(hours=2)).isoformat()
        }
        
        # Send to processing queue
        response = self.sqs.send_message(
            QueueUrl=self.processing_queue,
            MessageBody=json.dumps(job_message),
            MessageAttributes={
                'JobType': {
                    'StringValue': 'ThermalProcessing',
                    'DataType': 'String'
                },
                'Priority': {
                    'StringValue': 'HIGH',
                    'DataType': 'String'
                }
            }
        )
        
        # Store job in DynamoDB for tracking
        jobs_table = self.dynamodb.Table('ircamera-processing-jobs')
        jobs_table.put_item(
            Item={
                'job_id': job_message['job_id'],
                'session_id': job_message['session_id'],
                'status': 'QUEUED',
                'created_at': datetime.now().isoformat(),
                'sla_deadline': job_message['sla_deadline'],
                'processing_config': job_message['processing_config']
            }
        )
        
        return ProcessingJobResult(
            job_id=job_message['job_id'],
            queue_message_id=response['MessageId'],
            status='QUEUED'
        )
    
    async def setup_real_time_streaming(
        self, 
        session_id: str
    ) -> StreamingSetupResult:
        """Setup real-time data streaming to Kinesis"""
        
        try:
            # Create Kinesis stream if it doesn't exist
            try:
                self.kinesis.describe_stream(StreamName=self.data_stream)
            except ClientError as e:
                if e.response['Error']['Code'] == 'ResourceNotFoundException':
                    self.kinesis.create_stream(
                        StreamName=self.data_stream,
                        ShardCount=5
                    )
                    
                    # Wait for stream to become active
                    waiter = self.kinesis.get_waiter('stream_exists')
                    waiter.wait(StreamName=self.data_stream)
            
            # Setup stream producer configuration
            producer_config = {
                'stream_name': self.data_stream,
                'partition_key_prefix': f'session_{session_id}',
                'batch_size': 100,
                'linger_ms': 100,
                'compression': 'gzip'
            }
            
            return StreamingSetupResult(
                success=True,
                stream_name=self.data_stream,
                producer_config=producer_config
            )
            
        except Exception as e:
            return StreamingSetupResult(
                success=False,
                error=str(e)
            )
```

### Azure Enterprise Integration

```python
# Azure Enterprise Cloud Integration
from azure.storage.blob import BlobServiceClient, ContentSettings
from azure.servicebus import ServiceBusClient, ServiceBusMessage
from azure.eventhub import EventHubProducerClient, EventData
from azure.keyvault.secrets import SecretClient
from azure.identity import DefaultAzureCredential
import asyncio

class AzureEnterpriseIntegration:
    """Enterprise-grade Azure integration for IRCamera platform"""
    
    def __init__(self, azure_config: Dict[str, Any]):
        # Initialize Azure clients with managed identity
        credential = DefaultAzureCredential()
        
        self.blob_client = BlobServiceClient(
            account_url=azure_config['storage_account_url'],
            credential=credential
        )
        
        self.servicebus_client = ServiceBusClient(
            fully_qualified_namespace=azure_config['servicebus_namespace'],
            credential=credential
        )
        
        self.eventhub_client = EventHubProducerClient(
            fully_qualified_namespace=azure_config['eventhub_namespace'],
            eventhub_name=azure_config['eventhub_name'],
            credential=credential
        )
        
        self.keyvault_client = SecretClient(
            vault_url=azure_config['keyvault_url'],
            credential=credential
        )
        
        # Configuration
        self.container_name = azure_config['blob_container']
        self.processing_queue = azure_config['servicebus_queue']
        self.notification_topic = azure_config['servicebus_topic']
    
    async def upload_thermal_session_azure(
        self, 
        session_data: ThermalSessionData
    ) -> AzureUploadResult:
        """Enterprise-grade thermal session upload to Azure"""
        
        try:
            # Step 1: Get encryption key from Key Vault
            encryption_key = await self.get_encryption_key(session_data.session_id)
            
            # Step 2: Encrypt session data
            encrypted_session = await self.encrypt_with_azure_key(session_data, encryption_key)
            
            # Step 3: Upload to Blob Storage with enterprise features
            blob_result = await self.upload_to_blob_storage_enterprise(encrypted_session)
            
            # Step 4: Send to Service Bus for processing
            processing_message = await self.create_processing_message(blob_result)
            await self.send_to_service_bus(processing_message)
            
            # Step 5: Stream metadata to Event Hub
            metadata_event = await self.create_metadata_event(session_data, blob_result)
            await self.send_to_event_hub(metadata_event)
            
            return AzureUploadResult(
                session_id=session_data.session_id,
                blob_urls=blob_result.urls,
                processing_message_id=processing_message.message_id,
                event_hub_partition=metadata_event.partition_id
            )
            
        except Exception as e:
            await self.log_azure_error(session_data.session_id, e)
            return AzureUploadResult.error(str(e))
    
    async def upload_to_blob_storage_enterprise(
        self, 
        encrypted_session: EncryptedSessionData
    ) -> BlobUploadResult:
        """Upload to Azure Blob Storage with enterprise features"""
        
        urls = {}
        
        try:
            # Set content settings for thermal frames
            content_settings = ContentSettings(
                content_type='application/octet-stream',
                content_encoding='gzip',
                cache_control='no-cache'
            )
            
            # Upload thermal frames
            for i, frame in enumerate(encrypted_session.thermal_frames):
                blob_name = f"thermal-data/{encrypted_session.session_id}/frames/frame_{i:06d}.enc"
                
                blob_client = self.blob_client.get_blob_client(
                    container=self.container_name,
                    blob=blob_name
                )
                
                # Upload with metadata and tags
                blob_client.upload_blob(
                    frame.encrypted_data,
                    content_settings=content_settings,
                    metadata={
                        'session_id': encrypted_session.session_id,
                        'frame_index': str(i),
                        'timestamp': str(frame.timestamp),
                        'device_type': encrypted_session.device_type,
                        'data_classification': 'sensitive',
                        'retention_years': '7'
                    },
                    tags={
                        'project': 'ircamera',
                        'environment': 'production',
                        'data_type': 'thermal_frame'
                    },
                    overwrite=True
                )
                
                urls[f'frame_{i}'] = f"https://{self.blob_client.account_name}.blob.core.windows.net/{self.container_name}/{blob_name}"
            
            # Upload session metadata
            metadata_blob_name = f"thermal-data/{encrypted_session.session_id}/metadata.json"
            metadata_json = json.dumps(encrypted_session.metadata, indent=2)
            
            metadata_blob_client = self.blob_client.get_blob_client(
                container=self.container_name,
                blob=metadata_blob_name
            )
            
            metadata_blob_client.upload_blob(
                metadata_json,
                content_settings=ContentSettings(content_type='application/json'),
                metadata={
                    'session_id': encrypted_session.session_id,
                    'data_type': 'session_metadata',
                    'data_classification': 'sensitive'
                },
                overwrite=True
            )
            
            urls['metadata'] = f"https://{self.blob_client.account_name}.blob.core.windows.net/{self.container_name}/{metadata_blob_name}"
            
            return BlobUploadResult(
                success=True,
                urls=urls,
                encryption_key_id=encrypted_session.key_id
            )
            
        except Exception as e:
            return BlobUploadResult(
                success=False,
                error=str(e),
                urls=urls
            )
    
    async def send_to_service_bus(self, message: ProcessingMessage):
        """Send message to Service Bus for processing"""
        
        with self.servicebus_client:
            sender = self.servicebus_client.get_queue_sender(queue_name=self.processing_queue)
            
            with sender:
                # Create Service Bus message
                servicebus_message = ServiceBusMessage(
                    body=json.dumps(message.to_dict()),
                    content_type='application/json',
                    message_id=message.message_id,
                    correlation_id=message.session_id,
                    subject='ThermalProcessingJob',
                    application_properties={
                        'job_type': 'thermal_processing',
                        'priority': 'high',
                        'sla_hours': '2'
                    }
                )
                
                # Send message
                sender.send_messages(servicebus_message)
    
    async def send_to_event_hub(self, event: MetadataEvent):
        """Send real-time metadata to Event Hub"""
        
        async with self.eventhub_client:
            # Create event data
            event_data = EventData(
                body=json.dumps(event.to_dict()),
                content_type='application/json'
            )
            
            # Add properties
            event_data.properties = {
                'session_id': event.session_id,
                'event_type': 'session_upload',
                'timestamp': event.timestamp
            }
            
            # Send to Event Hub
            await self.eventhub_client.send_batch([event_data])
```

---

## Microservices Integration

### IRCamera Microservices Architecture

```mermaid
graph TB
    subgraph "API Gateway Layer"
        Gateway[API Gateway]
        Auth[Authentication Service]
        RateLimit[Rate Limiting]
    end
    
    subgraph "Core Services"
        SessionService[Session Management Service]
        DeviceService[Device Management Service]
        ProcessingService[Thermal Processing Service]
        StorageService[Storage Service]
        NotificationService[Notification Service]
    end
    
    subgraph "Data Services"
        MetadataDB[(Metadata Database)]
        TimeSeriesDB[(Time Series Database)]
        BlobStorage[(Blob Storage)]
        SearchIndex[(Search Index)]
    end
    
    subgraph "Integration Services"
        EventBus[Event Bus]
        MessageQueue[Message Queue]
        WorkflowEngine[Workflow Engine]
        AuditService[Audit Service]
    end
    
    subgraph "Monitoring & Observability"
        Metrics[Metrics Collector]
        Logs[Log Aggregator]
        Tracing[Distributed Tracing]
        Alerts[Alert Manager]
    end
    
    Gateway --> Auth
    Gateway --> RateLimit
    Gateway --> SessionService
    Gateway --> DeviceService
    Gateway --> ProcessingService
    
    SessionService --> MetadataDB
    SessionService --> EventBus
    
    DeviceService --> MetadataDB
    DeviceService --> EventBus
    
    ProcessingService --> TimeSeriesDB
    ProcessingService --> BlobStorage
    ProcessingService --> MessageQueue
    
    StorageService --> BlobStorage
    StorageService --> MetadataDB
    
    NotificationService --> EventBus
    NotificationService --> MessageQueue
    
    EventBus --> WorkflowEngine
    EventBus --> AuditService
    
    All Services --> Metrics
    All Services --> Logs
    All Services --> Tracing
```

### Microservice Implementation

```python
# IRCamera Session Management Microservice
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import asyncio
import aioredis
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
import uuid
from datetime import datetime, timedelta

app = FastAPI(title="IRCamera Session Management Service", version="1.0.0")

# Security
security = HTTPBearer()

# Database setup
DATABASE_URL = "postgresql+asyncpg://user:password@localhost/ircamera_sessions"
engine = create_async_engine(DATABASE_URL)
async_session_maker = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

# Redis for caching
redis_pool = None

@app.on_event("startup")
async def startup_event():
    global redis_pool
    redis_pool = aioredis.ConnectionPool.from_url("redis://localhost")

# Pydantic models
class SessionCreateRequest(BaseModel):
    device_ids: List[str]
    session_name: str
    description: Optional[str] = None
    recording_config: Dict[str, Any]
    metadata: Optional[Dict[str, Any]] = None

class SessionResponse(BaseModel):
    session_id: str
    session_name: str
    status: str
    device_count: int
    created_at: datetime
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    recording_config: Dict[str, Any]
    metadata: Optional[Dict[str, Any]] = None

class SessionMetrics(BaseModel):
    session_id: str
    total_frames: int
    total_data_size: int
    frame_rate: float
    data_quality_score: float
    error_count: int
    last_updated: datetime

# Dependency injection
async def get_db() -> AsyncSession:
    async with async_session_maker() as session:
        yield session

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    # JWT token validation logic here
    token = credentials.credentials
    user = await validate_jwt_token(token)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials"
        )
    return user

# Service layer
class SessionService:
    def __init__(self, db: AsyncSession, redis: aioredis.Redis):
        self.db = db
        self.redis = redis
        self.event_publisher = EventPublisher()
    
    async def create_session(
        self, 
        request: SessionCreateRequest, 
        user_id: str
    ) -> SessionResponse:
        """Create a new thermal imaging session"""
        
        session_id = str(uuid.uuid4())
        
        # Validate devices
        device_validation = await self.validate_devices(request.device_ids)
        if not device_validation.all_valid:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid devices: {device_validation.invalid_devices}"
            )
        
        # Create session in database
        session_entity = ThermalSession(
            session_id=session_id,
            session_name=request.session_name,
            description=request.description,
            user_id=user_id,
            device_ids=request.device_ids,
            status='CREATED',
            recording_config=request.recording_config,
            metadata=request.metadata,
            created_at=datetime.utcnow()
        )
        
        self.db.add(session_entity)
        await self.db.commit()
        
        # Cache session data
        await self.cache_session(session_entity)
        
        # Publish session created event
        await self.event_publisher.publish_event(
            event_type='session.created',
            session_id=session_id,
            data={
                'session_name': request.session_name,
                'device_count': len(request.device_ids),
                'user_id': user_id
            }
        )
        
        return SessionResponse(
            session_id=session_id,
            session_name=request.session_name,
            status='CREATED',
            device_count=len(request.device_ids),
            created_at=session_entity.created_at,
            recording_config=request.recording_config,
            metadata=request.metadata
        )
    
    async def start_session(self, session_id: str, user_id: str) -> SessionResponse:
        """Start a thermal imaging session"""
        
        # Get session from cache first
        session = await self.get_session_from_cache(session_id)
        if not session:
            session = await self.get_session_from_db(session_id)
            if not session:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Session not found"
                )
        
        # Validate session can be started
        if session.status != 'CREATED':
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Session cannot be started. Current status: {session.status}"
            )
        
        # Update session status
        session.status = 'STARTING'
        session.started_at = datetime.utcnow()
        
        # Update in database
        await self.update_session_in_db(session)
        
        # Update cache
        await self.cache_session(session)
        
        # Send start commands to devices
        device_commands = await self.prepare_device_start_commands(session)
        await self.send_device_commands(device_commands)
        
        # Publish session started event
        await self.event_publisher.publish_event(
            event_type='session.started',
            session_id=session_id,
            data={
                'device_count': len(session.device_ids),
                'recording_config': session.recording_config
            }
        )
        
        # Update status to active after successful device startup
        session.status = 'ACTIVE'
        await self.update_session_in_db(session)
        await self.cache_session(session)
        
        return SessionResponse.from_entity(session)
    
    async def get_session_metrics(self, session_id: str) -> SessionMetrics:
        """Get real-time session metrics"""
        
        # Try cache first
        metrics = await self.get_metrics_from_cache(session_id)
        if metrics:
            return metrics
        
        # Calculate metrics from database
        metrics = await self.calculate_session_metrics(session_id)
        
        # Cache for 30 seconds
        await self.cache_metrics(session_id, metrics, ttl=30)
        
        return metrics

# API endpoints
@app.post("/sessions", response_model=SessionResponse)
async def create_session(
    request: SessionCreateRequest,
    db: AsyncSession = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Create a new thermal imaging session"""
    redis = aioredis.Redis(connection_pool=redis_pool)
    service = SessionService(db, redis)
    
    return await service.create_session(request, current_user.user_id)

@app.post("/sessions/{session_id}/start", response_model=SessionResponse)
async def start_session(
    session_id: str,
    db: AsyncSession = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Start a thermal imaging session"""
    redis = aioredis.Redis(connection_pool=redis_pool)
    service = SessionService(db, redis)
    
    return await service.start_session(session_id, current_user.user_id)

@app.get("/sessions/{session_id}", response_model=SessionResponse)
async def get_session(
    session_id: str,
    db: AsyncSession = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Get session details"""
    redis = aioredis.Redis(connection_pool=redis_pool)
    service = SessionService(db, redis)
    
    session = await service.get_session(session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Session not found"
        )
    
    return session

@app.get("/sessions/{session_id}/metrics", response_model=SessionMetrics)
async def get_session_metrics(
    session_id: str,
    db: AsyncSession = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """Get real-time session metrics"""
    redis = aioredis.Redis(connection_pool=redis_pool)
    service = SessionService(db, redis)
    
    return await service.get_session_metrics(session_id)

@app.get("/sessions", response_model=List[SessionResponse])
async def list_sessions(
    status: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
    db: AsyncSession = Depends(get_db),
    current_user = Depends(get_current_user)
):
    """List sessions for the current user"""
    redis = aioredis.Redis(connection_pool=redis_pool)
    service = SessionService(db, redis)
    
    return await service.list_sessions(
        user_id=current_user.user_id,
        status=status,
        limit=limit,
        offset=offset
    )

# Health check
@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "service": "session-management",
        "version": "1.0.0"
    }
```

This comprehensive enterprise integration guide provides detailed patterns, implementations, and
strategies for deploying the IRCamera platform in enterprise environments with full cloud
integration, microservices architecture, and enterprise-grade security and compliance features.
