package com.gentics.mesh.core.data.user;


import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.user.UserReference;

public interface HibUser extends HibCoreElement, HibUserTracking, HibBucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.USER, USER_CREATED, USER_UPDATED, USER_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the username.
	 *
	 * @return Username
	 */
	String getUsername();

	/**
	 * Set the username.
	 *
	 * @param string
	 *            Username
	 * @return Fluent API
	 */
	HibUser setUsername(String string);

	/**
	 * Return the lastname.
	 *
	 * @return Lastname
	 */
	String getLastname();

	/**
	 * Set the lastname.
	 *
	 * @param lastname
	 * @return Fluent API
	 */
	HibUser setLastname(String lastname);

	/**
	 * Return the firstname.
	 *
	 * @return Firstname
	 */
	String getFirstname();

	/**
	 * Set the lastname.
	 *
	 * @param firstname
	 * @return Fluent API
	 */
	HibUser setFirstname(String firstname);

	/**
	 * Return the email address.
	 *
	 * @return Email address or null when no email address was set
	 */
	String getEmailAddress();

	/**
	 * Set the email address.
	 *
	 * @param email
	 *            Email address
	 * @return Fluent API
	 */
	HibUser setEmailAddress(String email);

	/**
	 * Return the password hash.
	 *
	 * @return Password hash
	 */
	String getPasswordHash();

	/**
	 * Disable the user.
	 */
	HibUser disable();

	/**
	 * Check whether the user is enabled.
	 *
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Enable the user.
	 */
	HibUser enable();

	UserReference transformToReference();

	Long getResetTokenIssueTimestamp();

	HibUser invalidateResetToken();

	HibUser setPasswordHash(String hash);

	/**
	 * Return true if the user needs to change their password on next login.
	 * 
	 * @return
	 */
	boolean isForcedPasswordChange();

	/**
	 * Set whether the user needs to change their password on next login.
	 * 
	 * @param force
	 * @return
	 */
	HibUser setForcedPasswordChange(boolean force);

	/**
	 * Return the admin flag of the user.
	 * 
	 * @return
	 */
	boolean isAdmin();

	/**
	 * Set the admin flag for the user.
	 * 
	 * @param flag
	 */
	void setAdmin(boolean flag);

	/**
	 * Return the currently stored API token id.
	 *
	 * @return API token id or null if no token has yet been generated.
	 */
	String getAPIKeyTokenCode();

	/**
	 * Return the timestamp when the api key token code was last issued.
	 *
	 * @return
	 */
	Long getAPITokenIssueTimestamp();

	/**
	 * Return an iterable of roles which belong to this user. Internally this will fetch all groups of the user and collect the assigned roles.
	 *
	 * @return
	 */
	// TODO we want to use this to the user dao
//	Iterable<? extends Role> getRoles();

	/**
	 * Reset the API token id and issue timestamp and thus invalidating the token.
	 */
	void resetAPIToken();

	/**
	 * Set the reset token for the user.
	 *
	 * @param token
	 * @return Fluent API
	 */
	HibUser setResetToken(String token);

	/**
	 * Set the user API token id.
	 *
	 * @param code
	 * @return Fluent API
	 */
	HibUser setAPITokenId(String code);

	/**
	 * Set the token code issue timestamp. This is used to influence the token expire moment.
	 *
	 * @param timestamp
	 * @return Fluent API
	 */
	HibUser setResetTokenIssueTimestamp(Long timestamp);

	/**
	 * Set the API token issue timestamp to the current time.
	 *
	 * @return Fluent API
	 */
	HibUser setAPITokenIssueTimestamp();

	/**
	 * Return the API token issue date.
	 *
	 * @return ISO8601 formatted date or null if the date has not yet been set
	 */
	String getAPITokenIssueDate();

	/**
	 * Return the currently stored reset token.
	 *
	 * @return Token or null if no token has been set or if the token has been used up
	 */
	String getResetToken();

	// Legacy - compat stuff

	/**
	 * Update all shortcut edges.
	 */
	void updateShortcutEdges();

	/**
	 * Delete the element.
	 */
	@Deprecated
	void remove();

	/**
	 * Return the referenced node which was assigned to the user.
	 *
	 * @return Referenced node or null when no node was assigned to the user.
	 */
	HibNode getReferencedNode();

	/**
	 * Set the referenced node.
	 *
	 * @param node
	 * @return Fluent API
	 */
	HibUser setReferencedNode(HibNode node);


	/**
	 * A CRC32 hash of the users {@link #getRoles roles}.
	 *
	 * @return A hash of the users roles
	 */
	String getRolesHash();

	String getElementVersion();

	MeshAuthUser toAuthUser();

}
