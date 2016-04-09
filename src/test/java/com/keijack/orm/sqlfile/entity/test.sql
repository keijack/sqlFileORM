--#count(ID)
select ID, name, CREATETIME as CT from TEST
	where 1 = 1
	and ID = :idEquals	and NAME like :nameLike@object[normal]
	and ID in (:idIn@list) 
	order by ID :idOrder@fragment[optional] -- :orderbyid@fragment