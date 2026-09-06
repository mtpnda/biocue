# Spec-First Development

Always prioritize the `.agents/SPEC/` directory as the single source of truth for all requirements, architecture, and UI behaviors.

## Mandatory Workflow Before Coding
1. **Analyze:** Whenever a user requests a new feature, logic change, or significant UI update, first check if the requirement is documented in `.agents/SPEC/`.
2. **Clarify:** If the request is vague, introduces edge cases, or conflicts with existing specifications, you MUST ask the user for clarification before writing any code. Do not make assumptions.
3. **Update SPEC:** Once requirements are clear, immediately update the relevant markdown files in `.agents/SPEC/` to reflect the new feature or logic.
4. **Code:** Only after the SPEC is updated and confirmed should you proceed to modify the application source code.
