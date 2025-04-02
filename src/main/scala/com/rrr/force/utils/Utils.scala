package com.rrr.force.utils

import com.typesafe.config.{Config, ConfigFactory}
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Trait for configuration parsing.
 *
 * Provides helper methods to load and retrieve configuration settings.
 */
trait ConfigParser {
  def config: Config

  def getString(key: String): String = config.getString(key)
  def getInt(key: String): Int = config.getInt(key)
  def getBoolean(key: String): Boolean = config.getBoolean(key)
  def getDouble(key: String): Double = config.getDouble(key)
}

/**
 * Default implementation of ConfigParser using Typesafe Config.
 */
object DefaultConfigParser extends ConfigParser {
  override val config: Config = ConfigFactory.load()
}

/**
 * Trait for logging utilities.
 *
 * This trait can be mixed into components that require logging.
 */
trait LoggerUtil {
  def logInfo(message: String): Unit
  def logError(message: String, throwable: Throwable): Unit
  def logDebug(message: String): Unit
}

/**
 * A simple console-based logger.
 *
 * In production, this might be replaced with a more robust logging framework.
 */
object ConsoleLogger extends LoggerUtil {
  override def logInfo(message: String): Unit = println(s"[INFO] $message")
  override def logError(message: String, throwable: Throwable): Unit = {
    println(s"[ERROR] $message")
    throwable.printStackTrace()
  }
  override def logDebug(message: String): Unit = println(s"[DEBUG] $message")
}

/**
 * Additional utility functions.
 *
 * This object contains helper functions such as timestamp formatting, etc.
 */
object Utils {
  /**
   * Formats an Instant to a human-readable string based on the provided pattern.
   *
   * @param instant The Instant to format.
   * @param pattern The pattern to use for formatting (default "yyyy-MM-dd HH:mm:ss").
   * @return        A formatted string representation of the Instant.
   */
  def formatInstant(instant: Instant, pattern: String = "yyyy-MM-dd HH:mm:ss"): String = {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    formatter.format(instant)
  }

  // Additional helper functions can be defined here.
}
