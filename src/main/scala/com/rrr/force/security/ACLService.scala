package com.rrr.force.security

/**
 * Trait for ACL (Access Control List) service.
 *
 * Provides methods to check if a given user role is authorized to perform a specific action.
 */
trait ACLService {

  /**
   * Checks whether the given role is authorized to perform the specified action.
   *
   * @param role   The user role (e.g., "data_analyst", "admin")
   * @param action The action to be performed (e.g., "execute_query", "modify_system")
   * @return true if authorized; false otherwise.
   */
  def isAuthorized(role: String, action: String): Boolean

  /**
   * Validates that the given role is authorized to perform the specified action.
   * Returns Right(()) if authorized, or a Left with an error message otherwise.
   *
   * @param role   The user role.
   * @param action The action to be performed.
   * @return Either an error message (Left) or Unit (Right) on successful authorization.
   */
  def authorize(role: String, action: String): Either[String, Unit] = {
    if (isAuthorized(role, action)) Right(())
    else Left(s"Role [$role] is not authorized to perform action [$action].")
  }
}

/**
 * Default implementation of ACLService.
 *
 * Uses a simple immutable Map to define which roles are allowed to perform which actions.
 */
object DefaultACLService extends ACLService {
  private val rolePermissions: Map[String, Set[String]] = Map(
    "data_analyst" -> Set("execute_query"),
    "admin"        -> Set("execute_query", "modify_system")
  )

  override def isAuthorized(role: String, action: String): Boolean =
    rolePermissions.getOrElse(role, Set.empty).contains(action)
}
