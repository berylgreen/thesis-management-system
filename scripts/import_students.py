import os
import re

# 配置路径
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_FILE = os.path.join(BASE_DIR, 'source', 'stu.md')
UPLOADS_DIR = os.path.join(BASE_DIR, 'backend', 'uploads')
SQL_OUTPUT_FILE = os.path.join(BASE_DIR, 'sql', 'import_students.sql')

# 默认密码 '123456' 的 BCrypt 哈希 (cost=10)
# 如果想要更安全，可以使用 python -c "import bcrypt; print(bcrypt.hashpw(b'123456', bcrypt.gensalt()).decode())" 生成新的
DEFAULT_PASSWORD_HASH = '$2a$10$7/0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0' # Placeholder, trying to generate real one below

# 尝试使用 bcrypt 生成哈希，如果没有安装则使用硬编码的 '123456' 哈希或者 'admin123' 的哈希
# 这里为了方便直接使用一个预生成的 '123456' hash (假设)
# 或者使用 init.sql 里的 admin123 哈希: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
PASSWORD_HASH = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' # Temporary using known hash (matches admin123)

def parse_markdown_table(file_path):
    students = []
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    for line in lines:
        # 匹配 Markdown 表格行: | 学号 | 姓名 | ...
        line = line.strip()
        if not line.startswith('|') or '---' in line:
            continue
            
        parts = [p.strip() for p in line.split('|')]
        # split 后 parts[0] 是空字符串 (因为行首有 |)
        if len(parts) >= 3:
            student_id = parts[1]
            student_name = parts[2]
            
            # 简单的验证：学号应该是数字
            if student_id and student_id.isdigit():
                students.append({
                    'id': student_id,
                    'name': student_name
                })
    return students

def generate_sql(students, output_file):
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("-- 自动生成的学生导入脚本\n")
        f.write("USE thesis_system;\n\n")
        f.write("INSERT IGNORE INTO t_user (username, password_hash, role, real_name) VALUES\n")
        
        values = []
        for s in students:
            # 只有当 username 不存在时插入
            values.append(f"('{s['id']}', '{PASSWORD_HASH}', 'STUDENT', '{s['name']}')")
            
        f.write(",\n".join(values) + ";\n")
    print(f"[SQL] 已生成 SQL 文件: {output_file} (包含 {len(students)} 条记录)")

def create_directories(students, base_upload_dir):
    if not os.path.exists(base_upload_dir):
        os.makedirs(base_upload_dir)
        print(f"[DIR] 创建上传目录: {base_upload_dir}")
    else:
        print(f"[DIR] 上传目录已存在: {base_upload_dir}")

def main():
    print("开始导入流程...")
    
    if not os.path.exists(SOURCE_FILE):
        print(f"错误: 找不到源文件 {SOURCE_FILE}")
        return

    students = parse_markdown_table(SOURCE_FILE)
    if not students:
        print("警告:而在文件中未找到有效的学生数据")
        return
        
    print(f"解析到 {len(students)} 名学生数据")
    
    # 1. 生成 SQL
    generate_sql(students, SQL_OUTPUT_FILE)
    
    # 2. 创建目录
    create_directories(students, UPLOADS_DIR)
    
    print("完成! 请手动或通过脚本执行 SQL 文件以更新数据库。")

if __name__ == '__main__':
    main()
