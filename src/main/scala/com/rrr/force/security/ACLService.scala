package com.rrr.force.security

import com.rrr.force.domain.QueryAST

/**
 * Authorizes queries based on roles/ACLs.
 */
trait ACLService {
  /**
   * Returns Right(()) if the AST is allowed under the current user/role, else Left(reason).
   */
  def authorize(ast: QueryAST): Either[String, Unit]
}

/**
 * A trivial allow‐all implementation.
 */
object DefaultACLService extends ACLService {
  override def authorize(ast: QueryAST): Either[String, Unit] = Right(())
}
