package com.ihm.hotelschool.branch;

import com.ihm.hotelschool.branch.dto.BranchResponse;
import org.springframework.stereotype.Component;

@Component
class BranchMapper {

	BranchResponse toResponse(Branch branch) {
		return new BranchResponse(
				branch.getId(),
				branch.getCode(),
				branch.getName(),
				branch.getAddress(),
				branch.getContactNumber(),
				branch.getStatus(),
				branch.isDefaultBranch(),
				branch.getCreatedAt(),
				branch.getUpdatedAt(),
				branch.getVersion());
	}
}
