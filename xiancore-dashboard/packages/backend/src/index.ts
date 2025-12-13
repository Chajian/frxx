import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import routes from '@/routes';

dotenv.config();

// 全局 BigInt 序列化支持
// @ts-ignore
BigInt.prototype.toJSON = function() {
  return this.toString();
};

const app = express();
const PORT = process.env.PORT || 8400;

// Middleware
app.use(cors({
  origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Request logging middleware
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  next();
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: Date.now() });
});

// API routes
app.use('/api', routes);

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    code: 404,
    message: 'API endpoint not found',
  });
});

// Error handler
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(500).json({
    code: 500,
    message: err.message || 'Internal server error',
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Server running on http://localhost:${PORT}`);
  console.log(`📊 Environment: ${process.env.NODE_ENV}`);
  console.log(`🔗 API endpoints:`);
  console.log(`   - GET  /health`);
  console.log(`   - GET  /api/players`);
  console.log(`   - GET  /api/players/ranking`);
  console.log(`   - GET  /api/players/:uuid`);
  console.log(`   - GET  /api/sects`);
  console.log(`   - GET  /api/sects/ranking`);
  console.log(`   - GET  /api/sects/:id`);
});
