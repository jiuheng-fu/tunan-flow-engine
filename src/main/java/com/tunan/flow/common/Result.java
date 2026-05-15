package com.tunan.flow.common;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * 通用返回对象
 */
@Data
public class Result<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer code;

	private Boolean success;

	private String msg;

	private T data;

	protected Result(Integer code, Boolean success,String message, T data) {
		this.code = code; // Changed from this.code = code; to handle Integer
		this.success = success;
		this.msg = message;
		this.data = data;
	}

	/**
	 * 成功返回结果
	 * @param data 获取的数据
	 */
	public static <T> Result<T> success(T data) {
		return new Result<>(ResultCode.SUCCESS.getCode(), true, ResultCode.SUCCESS.getMessage(),
				data);
	}

	/**
	 * 成功返回结果
	 */
	public static <T> Result<T> success() {
		return new Result<>(ResultCode.SUCCESS.getCode(),true, ResultCode.SUCCESS.getMessage(),
				null);
	}

	/**
	 * 成功返回结果
	 * @param data 获取的数据
	 * @param message 提示信息
	 */
	public static <T> Result<T> success(T data, String message) {
		return new Result<>(ResultCode.SUCCESS.getCode(), true, message, data);
	}

	/**
	 * 失败返回结果
	 * @param errorCode 错误码
	 */
	public static <T> Result<T> failed(IErrorCode errorCode) {
		return new Result<>(errorCode.getCode(), false, errorCode.getMessage(), null);
	}

	/**
	 * 失败返回结果
	 * @param message 提示信息
	 */
	public static <T> Result<T> failed(String message) {
		return new Result<>(ResultCode.FAILED.getCode(),false, message, null);
	}

	/**
	 * 失败返回结果
	 * @param message 提示信息
	 */
	public static <T> Result<T> failed(Integer code, String message) {
		return new Result<>(code,false, message, null);
	}

	/**
	 * 失败返回结果
	 */
	public static <T> Result<T> failed() {
		return failed(ResultCode.FAILED);
	}

	/**
	 * 参数验证失败返回结果
	 */
	public static <T> Result<T> validateFailed() {
		return failed(ResultCode.VALIDATE_FAILED);
	}

	/**
	 * 参数验证失败返回结果
	 * @param message 提示信息
	 */
	public static <T> Result<T> validateFailed(String message) {
		return new Result<>(ResultCode.VALIDATE_FAILED.getCode(),false, message, null);
	}

	/**
	 * 未登录返回结果
	 */
	public static <T> Result<T> unauthorized(T data) {
		return new Result<>(ResultCode.UNAUTHORIZED.getCode(),false,
				ResultCode.UNAUTHORIZED.getMessage(), data);
	}

	/**
	 * 未授权返回结果
	 */
	public static <T> Result<T> forbidden(T data) {
		return new Result<>(ResultCode.FORBIDDEN.getCode(),false, ResultCode.FORBIDDEN.getMessage(),
				data);
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>(4);
		map.put("code", code);
		map.put("success", success);
		map.put("message", msg);
		map.put("data", data);
		return map;
	}

}
