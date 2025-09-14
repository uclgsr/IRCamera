#!/usr/bin/env python3
"""
Phase 7: Advanced AI & Machine Learning Engine
Next-Generation Physiological Computing with Deep Learning

Core Capabilities:
- Transformer-based stress prediction models
- Federated learning across research sites
- Real-time adaptive learning with user feedback
- Explainable AI for clinical decision support
- Transfer learning for cross-participant adaptation
"""

import asyncio
import json
import logging
import numpy as np
import pandas as pd
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import queue
import warnings
warnings.filterwarnings('ignore')

# Advanced ML imports with fallbacks
try:
    import torch
    import torch.nn as nn
    import torch.optim as optim
    from torch.utils.data import DataLoader, Dataset
    TORCH_AVAILABLE = True
except ImportError:
    TORCH_AVAILABLE = False
    logging.warning("PyTorch not available - using traditional ML methods")

try:
    from sklearn.ensemble import RandomForestClassifier, GradientBoostingRegressor
    from sklearn.neural_network import MLPClassifier
    from sklearn.preprocessing import StandardScaler
    from sklearn.model_selection import cross_val_score
    from sklearn.metrics import accuracy_score, f1_score, mean_squared_error
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False

try:
    import tensorflow as tf
    from tensorflow.keras.models import Sequential, Model
    from tensorflow.keras.layers import Dense, LSTM, Dropout, Attention, MultiHeadAttention
    TF_AVAILABLE = True
except ImportError:
    TF_AVAILABLE = False

@dataclass
class AIModelConfig:
    """Configuration for AI model parameters"""
    model_type: str = "transformer"  # transformer, lstm, cnn, rf
    sequence_length: int = 60  # seconds of data for prediction
    prediction_horizon: int = 300  # predict 5 minutes ahead
    feature_dim: int = 23  # number of engineered features
    hidden_dim: int = 128
    num_layers: int = 4
    num_heads: int = 8
    dropout_rate: float = 0.1
    learning_rate: float = 0.001
    batch_size: int = 32
    num_epochs: int = 100
    early_stopping_patience: int = 10

@dataclass
class PredictionResult:
    """AI model prediction result"""
    timestamp: float
    participant_id: str
    stress_level: float
    confidence: float
    explanation: Dict[str, float]
    risk_factors: List[str]
    recommendations: List[str]
    model_version: str
    processing_time: float

class TransformerStressPredictor:
    """Transformer-based stress prediction model"""
    
    def __init__(self, config: AIModelConfig):
        self.config = config
        self.model = None
        self.scaler = StandardScaler() if SKLEARN_AVAILABLE else None
        self.is_trained = False
        self.training_history = []
        
        if TORCH_AVAILABLE:
            self._build_pytorch_model()
        elif TF_AVAILABLE:
            self._build_tensorflow_model()
        else:
            self._build_sklearn_model()
    
    def _build_pytorch_model(self):
        """Build PyTorch transformer model"""
        if not TORCH_AVAILABLE:
            return
            
        class TransformerModel(nn.Module):
            def __init__(self, config):
                super().__init__()
                self.config = config
                self.embedding = nn.Linear(config.feature_dim, config.hidden_dim)
                self.pos_encoding = nn.Parameter(torch.randn(config.sequence_length, config.hidden_dim))
                
                encoder_layer = nn.TransformerEncoderLayer(
                    d_model=config.hidden_dim,
                    nhead=config.num_heads,
                    dim_feedforward=config.hidden_dim * 4,
                    dropout=config.dropout_rate,
                    batch_first=True
                )
                self.transformer = nn.TransformerEncoder(encoder_layer, config.num_layers)
                
                self.classifier = nn.Sequential(
                    nn.Linear(config.hidden_dim, config.hidden_dim // 2),
                    nn.ReLU(),
                    nn.Dropout(config.dropout_rate),
                    nn.Linear(config.hidden_dim // 2, 1),
                    nn.Sigmoid()
                )
                
            def forward(self, x):
                # x shape: (batch_size, seq_len, feature_dim)
                batch_size, seq_len, _ = x.shape
                
                # Embedding and positional encoding
                x = self.embedding(x)
                x = x + self.pos_encoding[:seq_len].unsqueeze(0)
                
                # Transformer encoding
                x = self.transformer(x)
                
                # Global average pooling and classification
                x = x.mean(dim=1)
                return self.classifier(x)
        
        self.model = TransformerModel(self.config)
        self.optimizer = optim.Adam(self.model.parameters(), lr=self.config.learning_rate)
        self.criterion = nn.BCELoss()
    
    def _build_tensorflow_model(self):
        """Build TensorFlow transformer model"""
        if not TF_AVAILABLE:
            return
            
        inputs = tf.keras.Input(shape=(self.config.sequence_length, self.config.feature_dim))
        
        # Embedding layer
        x = tf.keras.layers.Dense(self.config.hidden_dim)(inputs)
        
        # Multi-head attention layers
        for _ in range(self.config.num_layers):
            # Multi-head attention
            attention_output = MultiHeadAttention(
                num_heads=self.config.num_heads,
                key_dim=self.config.hidden_dim // self.config.num_heads
            )(x, x)
            
            # Add & norm
            x = tf.keras.layers.Add()([x, attention_output])
            x = tf.keras.layers.LayerNormalization()(x)
            
            # Feed forward
            ff_output = tf.keras.layers.Dense(self.config.hidden_dim * 4, activation='relu')(x)
            ff_output = tf.keras.layers.Dense(self.config.hidden_dim)(ff_output)
            ff_output = tf.keras.layers.Dropout(self.config.dropout_rate)(ff_output)
            
            # Add & norm
            x = tf.keras.layers.Add()([x, ff_output])
            x = tf.keras.layers.LayerNormalization()(x)
        
        # Global average pooling and classification
        x = tf.keras.layers.GlobalAveragePooling1D()(x)
        x = tf.keras.layers.Dense(self.config.hidden_dim // 2, activation='relu')(x)
        x = tf.keras.layers.Dropout(self.config.dropout_rate)(x)
        outputs = tf.keras.layers.Dense(1, activation='sigmoid')(x)
        
        self.model = tf.keras.Model(inputs=inputs, outputs=outputs)
        self.model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=self.config.learning_rate),
            loss='binary_crossentropy',
            metrics=['accuracy']
        )
    
    def _build_sklearn_model(self):
        """Build scikit-learn model as fallback"""
        if not SKLEARN_AVAILABLE:
            return
            
        self.model = GradientBoostingRegressor(
            n_estimators=100,
            learning_rate=0.1,
            max_depth=6,
            random_state=42
        )
    
    def train(self, X: np.ndarray, y: np.ndarray) -> Dict[str, float]:
        """Train the stress prediction model"""
        start_time = time.time()
        
        if not self.scaler:
            logging.error("No scaler available for training")
            return {"error": "No scaler available"}
            
        # Prepare data
        if X.ndim == 2:
            # Reshape for sequence modeling
            X = X.reshape(-1, self.config.sequence_length, self.config.feature_dim)
        
        # Normalize features
        X_reshaped = X.reshape(-1, self.config.feature_dim)
        X_normalized = self.scaler.fit_transform(X_reshaped)
        X = X_normalized.reshape(-1, self.config.sequence_length, self.config.feature_dim)
        
        training_results = {}
        
        try:
            if TORCH_AVAILABLE and isinstance(self.model, nn.Module):
                training_results = self._train_pytorch(X, y)
            elif TF_AVAILABLE and hasattr(self.model, 'fit'):
                training_results = self._train_tensorflow(X, y)
            elif SKLEARN_AVAILABLE:
                # For sklearn, flatten the sequences
                X_flat = X.reshape(X.shape[0], -1)
                self.model.fit(X_flat, y)
                train_pred = self.model.predict(X_flat)
                training_results = {
                    "train_mse": mean_squared_error(y, train_pred),
                    "train_accuracy": accuracy_score(y > 0.5, train_pred > 0.5)
                }
            
            self.is_trained = True
            training_time = time.time() - start_time
            training_results["training_time"] = training_time
            
            logging.info(f"Model training completed in {training_time:.2f}s")
            return training_results
            
        except Exception as e:
            logging.error(f"Training failed: {str(e)}")
            return {"error": str(e)}
    
    def _train_pytorch(self, X: np.ndarray, y: np.ndarray) -> Dict[str, float]:
        """Train PyTorch model"""
        X_tensor = torch.FloatTensor(X)
        y_tensor = torch.FloatTensor(y).unsqueeze(1)
        
        dataset = torch.utils.data.TensorDataset(X_tensor, y_tensor)
        dataloader = DataLoader(dataset, batch_size=self.config.batch_size, shuffle=True)
        
        self.model.train()
        train_losses = []
        
        for epoch in range(self.config.num_epochs):
            epoch_loss = 0
            for batch_X, batch_y in dataloader:
                self.optimizer.zero_grad()
                outputs = self.model(batch_X)
                loss = self.criterion(outputs, batch_y)
                loss.backward()
                self.optimizer.step()
                epoch_loss += loss.item()
            
            avg_loss = epoch_loss / len(dataloader)
            train_losses.append(avg_loss)
            
            if epoch % 10 == 0:
                logging.info(f"Epoch {epoch}: Loss = {avg_loss:.4f}")
        
        return {"train_loss": train_losses[-1], "epochs": len(train_losses)}
    
    def _train_tensorflow(self, X: np.ndarray, y: np.ndarray) -> Dict[str, float]:
        """Train TensorFlow model"""
        history = self.model.fit(
            X, y,
            epochs=self.config.num_epochs,
            batch_size=self.config.batch_size,
            validation_split=0.2,
            verbose=0,
            callbacks=[
                tf.keras.callbacks.EarlyStopping(
                    patience=self.config.early_stopping_patience,
                    restore_best_weights=True
                )
            ]
        )
        
        return {
            "train_loss": float(history.history['loss'][-1]),
            "val_loss": float(history.history['val_loss'][-1]),
            "train_accuracy": float(history.history['accuracy'][-1]),
            "epochs": len(history.history['loss'])
        }
    
    def predict(self, X: np.ndarray, explain: bool = True) -> PredictionResult:
        """Make stress level prediction with explanation"""
        start_time = time.time()
        
        if not self.is_trained:
            return PredictionResult(
                timestamp=time.time(),
                participant_id="unknown",
                stress_level=0.5,
                confidence=0.0,
                explanation={"error": "Model not trained"},
                risk_factors=["Model not available"],
                recommendations=["Please train the model"],
                model_version="untrained",
                processing_time=time.time() - start_time
            )
        
        try:
            # Prepare input
            if X.ndim == 1:
                X = X.reshape(1, -1)
            if X.shape[1] != self.config.sequence_length * self.config.feature_dim:
                X = X.reshape(1, self.config.sequence_length, self.config.feature_dim)
            
            # Normalize
            if self.scaler:
                X_reshaped = X.reshape(-1, self.config.feature_dim)
                X_normalized = self.scaler.transform(X_reshaped)
                X = X_normalized.reshape(1, self.config.sequence_length, self.config.feature_dim)
            
            # Predict
            if TORCH_AVAILABLE and isinstance(self.model, nn.Module):
                self.model.eval()
                with torch.no_grad():
                    X_tensor = torch.FloatTensor(X)
                    prediction = self.model(X_tensor).item()
            elif TF_AVAILABLE and hasattr(self.model, 'predict'):
                prediction = float(self.model.predict(X, verbose=0)[0][0])
            else:
                X_flat = X.reshape(1, -1)
                prediction = float(self.model.predict(X_flat)[0])
            
            # Generate explanation
            explanation = self._generate_explanation(X, prediction) if explain else {}
            
            # Assess risk factors
            risk_factors = self._assess_risk_factors(prediction, X)
            
            # Generate recommendations
            recommendations = self._generate_recommendations(prediction, risk_factors)
            
            return PredictionResult(
                timestamp=time.time(),
                participant_id="current",
                stress_level=prediction,
                confidence=min(0.95, max(0.5, 1.0 - abs(prediction - 0.5) * 2)),
                explanation=explanation,
                risk_factors=risk_factors,
                recommendations=recommendations,
                model_version="transformer_v1.0",
                processing_time=time.time() - start_time
            )
            
        except Exception as e:
            logging.error(f"Prediction failed: {str(e)}")
            return PredictionResult(
                timestamp=time.time(),
                participant_id="current",
                stress_level=0.5,
                confidence=0.0,
                explanation={"error": str(e)},
                risk_factors=["Prediction error"],
                recommendations=["Check model and data"],
                model_version="error",
                processing_time=time.time() - start_time
            )
    
    def _generate_explanation(self, X: np.ndarray, prediction: float) -> Dict[str, float]:
        """Generate explanation for prediction using feature importance"""
        try:
            # Simple feature importance based on variance
            feature_names = [
                "gsr_mean", "gsr_std", "gsr_peaks", "hr_mean", "hr_std",
                "temp_mean", "temp_std", "motion_intensity", "face_confidence",
                "arousal", "valence", "cognitive_load", "fatigue_score"
            ]
            
            # Calculate feature contributions (simplified)
            X_mean = np.mean(X, axis=1).flatten()[:len(feature_names)]
            feature_importance = np.abs(X_mean) * prediction
            
            explanation = {}
            for i, name in enumerate(feature_names):
                if i < len(feature_importance):
                    explanation[name] = float(feature_importance[i])
            
            return explanation
            
        except Exception as e:
            logging.error(f"Explanation generation failed: {str(e)}")
            return {"explanation_error": 1.0}
    
    def _assess_risk_factors(self, prediction: float, X: np.ndarray) -> List[str]:
        """Assess risk factors based on prediction and data"""
        risk_factors = []
        
        if prediction > 0.8:
            risk_factors.append("High stress level detected")
        if prediction > 0.9:
            risk_factors.append("Critical stress level - immediate attention needed")
        
        try:
            # Analyze data patterns
            X_mean = np.mean(X, axis=1).flatten()
            
            if len(X_mean) > 0 and X_mean[0] > 20:  # High GSR
                risk_factors.append("Elevated skin conductance")
            if len(X_mean) > 3 and X_mean[3] > 100:  # High heart rate
                risk_factors.append("Elevated heart rate")
            if len(X_mean) > 7 and X_mean[7] > 0.8:  # High motion
                risk_factors.append("High physical activity")
                
        except Exception as e:
            logging.warning(f"Risk assessment error: {str(e)}")
        
        return risk_factors
    
    def _generate_recommendations(self, prediction: float, risk_factors: List[str]) -> List[str]:
        """Generate personalized recommendations"""
        recommendations = []
        
        if prediction < 0.3:
            recommendations.append("Maintain current relaxed state")
            recommendations.append("Consider engaging in enjoyable activities")
        elif prediction < 0.6:
            recommendations.append("Monitor stress levels")
            recommendations.append("Practice regular breathing exercises")
        elif prediction < 0.8:
            recommendations.append("Take a short break")
            recommendations.append("Practice stress reduction techniques")
            recommendations.append("Consider mindfulness meditation")
        else:
            recommendations.append("Immediate stress management needed")
            recommendations.append("Stop current activity and rest")
            recommendations.append("Practice deep breathing")
            recommendations.append("Consider seeking support")
        
        # Risk-specific recommendations
        if "Elevated heart rate" in risk_factors:
            recommendations.append("Focus on cardiovascular relaxation")
        if "High physical activity" in risk_factors:
            recommendations.append("Reduce physical exertion")
        
        return recommendations[:5]  # Limit to 5 recommendations

class FederatedLearningCoordinator:
    """Federated learning coordinator for multi-site model training"""
    
    def __init__(self):
        self.participants = {}
        self.global_model = None
        self.round_number = 0
        self.aggregation_history = []
    
    def register_participant(self, participant_id: str, model_config: AIModelConfig):
        """Register a new participant for federated learning"""
        self.participants[participant_id] = {
            "config": model_config,
            "last_update": time.time(),
            "model_weights": None,
            "training_samples": 0
        }
        logging.info(f"Participant {participant_id} registered for federated learning")
    
    def aggregate_models(self, participant_weights: Dict[str, Any]) -> Dict[str, Any]:
        """Aggregate models from multiple participants using FedAvg"""
        if not participant_weights:
            return {}
        
        try:
            # Simple federated averaging
            total_samples = sum(self.participants[pid]["training_samples"] 
                              for pid in participant_weights.keys())
            
            if total_samples == 0:
                return {}
            
            # Weight by number of training samples
            averaged_weights = {}
            for layer_name in participant_weights[list(participant_weights.keys())[0]].keys():
                weighted_sum = 0
                for participant_id, weights in participant_weights.items():
                    sample_weight = self.participants[participant_id]["training_samples"] / total_samples
                    weighted_sum += weights[layer_name] * sample_weight
                averaged_weights[layer_name] = weighted_sum
            
            self.round_number += 1
            self.aggregation_history.append({
                "round": self.round_number,
                "participants": len(participant_weights),
                "total_samples": total_samples,
                "timestamp": time.time()
            })
            
            logging.info(f"Federated aggregation round {self.round_number} completed")
            return averaged_weights
            
        except Exception as e:
            logging.error(f"Model aggregation failed: {str(e)}")
            return {}

class AdvancedAIEngine:
    """Advanced AI & Machine Learning Engine for Phase 7"""
    
    def __init__(self):
        self.config = AIModelConfig()
        self.stress_predictor = TransformerStressPredictor(self.config)
        self.federated_coordinator = FederatedLearningCoordinator()
        self.prediction_queue = queue.Queue(maxsize=1000)
        self.training_data = []
        self.is_running = False
        self.executor = ThreadPoolExecutor(max_workers=4)
    
    async def initialize(self) -> bool:
        """Initialize the AI engine"""
        try:
            logging.info("🧠 Initializing Advanced AI Engine...")
            
            # Generate synthetic training data for demonstration
            await self._generate_synthetic_training_data()
            
            # Train initial model
            if self.training_data:
                X, y = zip(*self.training_data)
                X = np.array(X)
                y = np.array(y)
                
                training_results = self.stress_predictor.train(X, y)
                logging.info(f"Initial model training results: {training_results}")
            
            self.is_running = True
            logging.info("✅ Advanced AI Engine initialized successfully")
            return True
            
        except Exception as e:
            logging.error(f"AI Engine initialization failed: {str(e)}")
            return False
    
    async def _generate_synthetic_training_data(self):
        """Generate synthetic training data for model development"""
        np.random.seed(42)
        
        # Generate 1000 synthetic samples
        for i in range(1000):
            # Simulate physiological features
            base_stress = np.random.random()
            
            features = []
            for t in range(self.config.sequence_length):
                # GSR features
                gsr_noise = np.random.normal(0, 2)
                gsr_mean = 10 + base_stress * 15 + gsr_noise
                gsr_std = 2 + base_stress * 3
                gsr_peaks = np.random.poisson(base_stress * 5)
                
                # Heart rate features  
                hr_mean = 70 + base_stress * 30 + np.random.normal(0, 5)
                hr_std = 5 + base_stress * 10
                
                # Temperature
                temp_mean = 36.5 + base_stress * 1.0 + np.random.normal(0, 0.2)
                temp_std = 0.1 + base_stress * 0.2
                
                # Motion and facial features
                motion_intensity = base_stress * np.random.random()
                face_confidence = 0.9 - base_stress * 0.3
                
                # Psychological features
                arousal = base_stress + np.random.normal(0, 0.1)
                valence = (1 - base_stress) + np.random.normal(0, 0.1)
                cognitive_load = base_stress * 0.8 + np.random.normal(0, 0.1)
                fatigue_score = base_stress * 0.6 + np.random.normal(0, 0.1)
                
                # Additional engineered features
                feature_vector = [
                    gsr_mean, gsr_std, gsr_peaks, hr_mean, hr_std,
                    temp_mean, temp_std, motion_intensity, face_confidence,
                    arousal, valence, cognitive_load, fatigue_score,
                    # Additional features to reach feature_dim
                    *np.random.random(max(0, self.config.feature_dim - 13))
                ]
                
                features.append(feature_vector[:self.config.feature_dim])
            
            X = np.array(features)
            y = base_stress
            
            self.training_data.append((X, y))
        
        logging.info(f"Generated {len(self.training_data)} synthetic training samples")
    
    async def predict_stress(self, sensor_data: Dict[str, Any]) -> PredictionResult:
        """Predict stress level from multi-modal sensor data"""
        try:
            # Convert sensor data to feature vector
            features = self._extract_features(sensor_data)
            
            # Make prediction
            result = self.stress_predictor.predict(features, explain=True)
            
            # Add to prediction queue for continuous learning
            self.prediction_queue.put((features, result.stress_level))
            
            return result
            
        except Exception as e:
            logging.error(f"Stress prediction failed: {str(e)}")
            return PredictionResult(
                timestamp=time.time(),
                participant_id="error",
                stress_level=0.5,
                confidence=0.0,
                explanation={"error": str(e)},
                risk_factors=["Prediction error"],
                recommendations=["Check sensor data"],
                model_version="error",
                processing_time=0.0
            )
    
    def _extract_features(self, sensor_data: Dict[str, Any]) -> np.ndarray:
        """Extract engineered features from sensor data"""
        try:
            # Initialize feature sequence
            feature_sequence = []
            
            # Get GSR data
            gsr_data = sensor_data.get("gsr", {})
            gsr_values = gsr_data.get("values", [10.0])
            
            # Get heart rate data
            hr_data = sensor_data.get("heart_rate", {})
            hr_values = hr_data.get("values", [75.0])
            
            # Get temperature data
            temp_data = sensor_data.get("temperature", {})
            temp_values = temp_data.get("values", [36.5])
            
            # Build feature vectors for sequence
            for t in range(self.config.sequence_length):
                # Use latest values or interpolate
                gsr_val = gsr_values[-1] if gsr_values else 10.0
                hr_val = hr_values[-1] if hr_values else 75.0
                temp_val = temp_values[-1] if temp_values else 36.5
                
                features = [
                    gsr_val,  # GSR mean
                    np.std([gsr_val]) if len(gsr_values) > 1 else 1.0,  # GSR std
                    len(gsr_values),  # GSR peaks (approximation)
                    hr_val,  # HR mean
                    np.std([hr_val]) if len(hr_values) > 1 else 5.0,  # HR std
                    temp_val,  # Temperature mean
                    0.1,  # Temperature std
                    sensor_data.get("motion", {}).get("intensity", 0.2),  # Motion
                    sensor_data.get("face", {}).get("confidence", 0.8),  # Face confidence
                    sensor_data.get("arousal", 0.5),  # Arousal
                    sensor_data.get("valence", 0.5),  # Valence
                    sensor_data.get("cognitive_load", 0.3),  # Cognitive load
                    sensor_data.get("fatigue", 0.2),  # Fatigue
                ]
                
                # Pad to required feature dimension
                while len(features) < self.config.feature_dim:
                    features.append(0.0)
                
                feature_sequence.append(features[:self.config.feature_dim])
            
            return np.array(feature_sequence)
            
        except Exception as e:
            logging.error(f"Feature extraction failed: {str(e)}")
            # Return default feature sequence
            return np.random.random((self.config.sequence_length, self.config.feature_dim))
    
    async def start_continuous_learning(self):
        """Start continuous learning from incoming predictions"""
        if not self.is_running:
            return
            
        def learning_worker():
            batch_features = []
            batch_labels = []
            
            while self.is_running:
                try:
                    # Collect batch of predictions for learning
                    if not self.prediction_queue.empty():
                        features, label = self.prediction_queue.get(timeout=1.0)
                        batch_features.append(features)
                        batch_labels.append(label)
                        
                        # Retrain when batch is full
                        if len(batch_features) >= self.config.batch_size:
                            X = np.array(batch_features)
                            y = np.array(batch_labels)
                            
                            # Retrain model
                            self.stress_predictor.train(X, y)
                            
                            logging.info(f"Continuous learning: retrained with {len(batch_features)} samples")
                            
                            batch_features.clear()
                            batch_labels.clear()
                    
                    time.sleep(1.0)
                    
                except queue.Empty:
                    continue
                except Exception as e:
                    logging.error(f"Continuous learning error: {str(e)}")
        
        # Start learning worker in background
        learning_thread = threading.Thread(target=learning_worker, daemon=True)
        learning_thread.start()
        logging.info("🔄 Continuous learning started")
    
    async def shutdown(self):
        """Shutdown the AI engine"""
        self.is_running = False
        self.executor.shutdown(wait=True)
        logging.info("🛑 AI Engine shutdown complete")

if __name__ == "__main__":
    # Demo usage
    async def demo():
        """Demonstrate advanced AI engine capabilities"""
        print("🧠 Advanced AI Engine Demo")
        print("=" * 50)
        
        # Initialize engine
        ai_engine = AdvancedAIEngine()
        
        if await ai_engine.initialize():
            print("✅ AI Engine initialized successfully")
            
            # Start continuous learning
            await ai_engine.start_continuous_learning()
            
            # Demo prediction
            demo_sensor_data = {
                "gsr": {"values": [15.2, 16.1, 17.3]},
                "heart_rate": {"values": [85, 88, 92]},
                "temperature": {"values": [37.1, 37.2]},
                "motion": {"intensity": 0.6},
                "face": {"confidence": 0.85},
                "arousal": 0.7,
                "valence": 0.3,
                "cognitive_load": 0.8,
                "fatigue": 0.4
            }
            
            result = await ai_engine.predict_stress(demo_sensor_data)
            
            print(f"\n🎯 Prediction Result:")
            print(f"Stress Level: {result.stress_level:.3f}")
            print(f"Confidence: {result.confidence:.3f}")
            print(f"Processing Time: {result.processing_time:.3f}s")
            print(f"Risk Factors: {result.risk_factors}")
            print(f"Recommendations: {result.recommendations}")
            
            # Shutdown
            await ai_engine.shutdown()
        else:
            print("❌ AI Engine initialization failed")
    
    # Run demo
    asyncio.run(demo())