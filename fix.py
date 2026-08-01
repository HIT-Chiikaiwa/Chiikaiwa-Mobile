import os

base_dir = r"e:\HIT_Product\app\src\main\java\com\example\myapplication"

# Fix AuthInterceptor.kt package
ai_file = os.path.join(base_dir, r"data\remote\interceptor\AuthInterceptor.kt")
with open(ai_file, "r", encoding="utf-8") as f:
    ai_content = f.read()
ai_content = ai_content.replace("package com.example.myapplication.data.remote.interceptor.interceptor", "package com.example.myapplication.data.remote.interceptor")
with open(ai_file, "w", encoding="utf-8") as f:
    f.write(ai_content)

def add_import(filepath, import_statement):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    if import_statement not in content:
        lines = content.split('\n')
        # find the last import
        last_import = -1
        for i, line in enumerate(lines):
            if line.startswith('import '):
                last_import = i
        
        if last_import != -1:
            lines.insert(last_import + 1, import_statement)
        else:
            lines.insert(1, "\n" + import_statement)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(lines))

add_import(os.path.join(base_dir, r"data\remote\network\RetrofitClient.kt"), "import com.example.myapplication.data.remote.api.ApiService")
add_import(os.path.join(base_dir, r"data\repository\AuthRepository.kt"), "import com.example.myapplication.data.remote.api.ApiService")
add_import(os.path.join(base_dir, r"data\repository\MapRepository.kt"), "import com.example.myapplication.data.remote.api.ApiService")
add_import(os.path.join(base_dir, r"data\repository\ProfileRepository.kt"), "import com.example.myapplication.data.remote.api.ApiService")

print("Fixed imports and packages")
