package com.gentics.mesh.core.rest.error;

public class PermissionException extends AbstractUnavailableException {

	public static final String TYPE = "missing_perm";

	private static final long serialVersionUID = 6097093959066715614L;
	public static final String i18nKey = "graphql_error_missing_perm";

	public PermissionException(String elementType, String elementId) {
		super(null, i18nKey, elementType, elementId);
	}

	@Override
	public String getType() {
		return TYPE;
	}

}
