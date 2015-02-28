select * from TEST
	-- single line comments
	where 1 = 1
	/**
	 * multi line comments
	 */
	--#idEqual:
	and id = ?
	--idEqual#
	--#nameLike:
	and name like ?
	--nameLike#
	--#idIn@required:
	and id in (?)
	--idIn#