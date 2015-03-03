--#count(ID)
select ID, NAME, CREATETIME as CT from TEST
	where 1 = 1
	and ID = :idEquals	and NAME like :nameLike
	and ID in (:idIn@required) 
	order by ID :idOrder@optionalString -- :orderbyid@string