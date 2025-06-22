# 📋 Pull Request Templates Guide

This directory contains industry-standard pull request templates for the Weather Service project. These templates help ensure consistent, high-quality pull requests and streamline the code review process.

## 🎯 Available Templates

### **📄 Default Template** ([pull_request_template.md](../pull_request_template.md))
- **Use for**: General pull requests
- **Features**: Comprehensive checklist covering all aspects of development
- **Auto-loaded**: Automatically used when creating new PRs

### **✨ Feature Template** ([feature.md](./feature.md))
- **Use for**: New feature development
- **Query parameter**: `?template=feature.md`
- **Focus areas**: Business value, architecture, API design, testing

### **🐛 Bug Fix Template** ([bugfix.md](./bugfix.md))
- **Use for**: Bug fixes and issue resolution
- **Query parameter**: `?template=bugfix.md`  
- **Focus areas**: Root cause analysis, fix validation, risk assessment

### **🔧 Configuration Template** ([config.md](./config.md))
- **Use for**: Configuration and infrastructure changes
- **Query parameter**: `?template=config.md`
- **Focus areas**: Environment impact, deployment planning, rollback strategy

### **⚡ Performance Template** ([performance.md](./performance.md))
- **Use for**: Performance optimizations and improvements
- **Query parameter**: `?template=performance.md`
- **Focus areas**: Benchmarking, testing methodology, monitoring

## 🚀 How to Use Templates

### **Method 1: GitHub Web Interface**
1. Navigate to your repository on GitHub
2. Create a new pull request
3. Add the template query parameter to the URL:
   ```
   https://github.com/your-org/weather-service/compare/main...your-branch?template=feature.md
   ```

### **Method 2: Direct Links**
Use these direct links when creating PRs:

- **Feature**: `https://github.com/your-org/weather-service/compare/main...your-branch?template=feature.md`
- **Bug Fix**: `https://github.com/your-org/weather-service/compare/main...your-branch?template=bugfix.md`  
- **Configuration**: `https://github.com/your-org/weather-service/compare/main...your-branch?template=config.md`
- **Performance**: `https://github.com/your-org/weather-service/compare/main...your-branch?template=performance.md`

### **Method 3: Template Selection**
GitHub will show available templates when you create a PR if multiple templates exist.

## 📋 Template Usage Guidelines

### **✅ Best Practices**
1. **Choose the Right Template**: Select the template that best matches your change type
2. **Fill All Sections**: Complete all relevant sections thoroughly
3. **Use Checkboxes**: Check off completed items as you go
4. **Be Specific**: Provide detailed information rather than generic descriptions
5. **Include Evidence**: Add screenshots, logs, or test results where applicable

### **📊 Quality Checklist**
Before submitting your PR, ensure:

- [ ] **Appropriate template selected**
- [ ] **All required sections completed**
- [ ] **Testing strategy documented**
- [ ] **Review focus areas specified**
- [ ] **Related issues linked**
- [ ] **Documentation updated**

### **🔍 Review Guidelines**

#### **For Authors**
- Use templates to ensure completeness
- Provide context for reviewers
- Highlight areas needing special attention
- Include testing evidence

#### **For Reviewers**
- Check that appropriate template was used
- Verify all checklist items are addressed
- Focus on areas highlighted by the author
- Ensure testing is adequate
- **Follow the comprehensive testing guide**: [REVIEWER_TESTING_GUIDE.md](../REVIEWER_TESTING_GUIDE.md)

## 🏗️ Template Customization

### **Adding New Templates**
1. Create new `.md` file in this directory
2. Follow the established format and structure
3. Include comprehensive checklists
4. Update this README with the new template

### **Modifying Templates**
1. Consider impact on existing PRs
2. Maintain backward compatibility where possible
3. Update documentation accordingly
4. Communicate changes to the team

## 📚 Template Structure

All templates follow this consistent structure:

1. **Header**: Clear title and summary
2. **Type Classification**: Checkboxes for change type
3. **Detailed Description**: Context and implementation details
4. **Testing Strategy**: Comprehensive testing approach
5. **Quality Checklist**: Code quality and standards
6. **Documentation**: Required documentation updates
7. **Review Focus**: Areas for reviewer attention
8. **Links**: Related issues and dependencies

## 🎯 Weather Service Specific Guidelines

### **Reactive Programming**
- Ensure BlockHound compliance for reactive code
- Verify no blocking calls in reactive chains
- Test backpressure handling appropriately

### **Environment Configuration**
- Test across all environment profiles (local, dev, qa, prod)
- Verify Netty configuration impacts
- Check resource utilization changes

### **Performance Standards**
- Include performance impact assessment
- Provide benchmarking results for significant changes
- Monitor resource usage implications

### **Documentation Requirements**
- Update relevant technical guides (BlockHound, Netty)
- Maintain architecture decision records
- Keep checklists current

## 🔗 Related Resources

- **[Contributing Guide](../../docs/CONTRIBUTING.md)** (if available)
- **[Code Review Guidelines](../../docs/CODE_REVIEW.md)** (if available)
- **[BlockHound Guide](../../docs/BLOCKHOUND-GUIDE.md)**
- **[Netty Configuration Guide](../../docs/NETTY-CONFIGURATION-GUIDE.md)**
- **[Architecture Documentation](../../docs/architecture/)**

---

**📋 For questions about PR templates, create an issue or discuss in team channels.**