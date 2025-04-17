// src/main/scala/com/rrr/force/domain/BroadcastModels.scala
package com.rrr.force.domain

/**
 * Represents a user in the broadcast dataset (small dimension table).
 * Contains enrichment fields for joins.
 *
 * @param id    Unique user identifier
 * @param login Username (join key)
 * @param name  Optional display name
 * @param email Optional email address
 */
case class BroadcastUser(
                          id: Long,
                          login: String,
                          name: Option[String],
                          email: Option[String]
                        )

/**
 * Represents an organization in the broadcast dataset.
 * Contains enrichment fields for joins.
 *
 * @param id    Unique organization identifier
 * @param login Organization login (join key)
 * @param name  Optional full org name
 */
case class Organization(
                         id: Long,
                         login: String,
                         name: Option[String]
                       )
