select ID, NAME, CREATETIME as CT from TEST
	where 1 = 1
	--#idEqual:
	/**
	 * multi line comments
	 */
	and ID = ?
	--idEqual#
	--#nameLike:
	-- single line comments
	and NAME like ?
	--nameLike#
	--#idIn@required:
	and ID in (?)
	--idIn#