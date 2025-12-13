import { Response } from 'express';

/**
 * 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data?: T;
}

/**
 * 成功响应
 */
export function success<T>(res: Response, data: T, message = 'success') {
  const response: ApiResponse<T> = {
    code: 0,
    message,
    data,
  };
  return res.json(response);
}

/**
 * 错误响应
 */
export function error(res: Response, message: string, code = 500) {
  const response: ApiResponse = {
    code,
    message,
  };
  return res.status(code >= 500 ? 500 : 400).json(response);
}
