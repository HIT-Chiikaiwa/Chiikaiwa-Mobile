import os
import shutil

base_dir = r"e:\HIT_Product\app\src\main\java\com\example\myapplication"

data_dir = os.path.join(base_dir, "data")
remote_dir = os.path.join(data_dir, "remote")
model_dir = os.path.join(data_dir, "model")
utils_dir = os.path.join(base_dir, "utils")

# 1. Rename directory respository -> repository
repo_old = os.path.join(data_dir, "respository")
repo_new = os.path.join(data_dir, "repository")
if os.path.exists(repo_old) and not os.path.exists(repo_new):
    os.rename(repo_old, repo_new)

# 2. Rename files in repository
if os.path.exists(repo_new):
    for f in os.listdir(repo_new):
        if "Respository" in f:
            new_f = f.replace("Respository", "Repository")
            os.rename(os.path.join(repo_new, f), os.path.join(repo_new, new_f))

# 3. Create remote subdirectories
api_dir = os.path.join(remote_dir, "api")
network_dir = os.path.join(remote_dir, "network")
interceptor_dir = os.path.join(remote_dir, "interceptor")
dto_dir = os.path.join(remote_dir, "dto")

for d in [api_dir, network_dir, interceptor_dir, dto_dir]:
    os.makedirs(d, exist_ok=True)

# 4. Move files in remote
api_file = os.path.join(remote_dir, "ApiService.kt")
if os.path.exists(api_file): shutil.move(api_file, os.path.join(api_dir, "ApiService.kt"))

net_file = os.path.join(remote_dir, "RetrofitClient.kt")
if os.path.exists(net_file): shutil.move(net_file, os.path.join(network_dir, "RetrofitClient.kt"))

auth_int_file = os.path.join(remote_dir, "AuthInterceptor.kt")
if os.path.exists(auth_int_file): shutil.move(auth_int_file, os.path.join(interceptor_dir, "AuthInterceptor.kt"))

# 5. Move request/response to dto
req_dir = os.path.join(model_dir, "request")
res_dir = os.path.join(model_dir, "response")
if os.path.exists(req_dir): shutil.move(req_dir, os.path.join(dto_dir, "request"))
if os.path.exists(res_dir): shutil.move(res_dir, os.path.join(dto_dir, "response"))

# 6. Create utils subdirectories
common_dir = os.path.join(utils_dir, "common")
resource_dir = os.path.join(utils_dir, "resource")
os.makedirs(common_dir, exist_ok=True)
os.makedirs(resource_dir, exist_ok=True)

# 7. Move utils files
sle_file = os.path.join(utils_dir, "SingleLiveEvent.kt")
if os.path.exists(sle_file): shutil.move(sle_file, os.path.join(common_dir, "SingleLiveEvent.kt"))

res_file = os.path.join(utils_dir, "Resource.kt")
if os.path.exists(res_file): shutil.move(res_file, os.path.join(resource_dir, "Resource.kt"))

# replacements mappings
replacements = {
    # Packages
    "package com.example.myapplication.data.respository": "package com.example.myapplication.data.repository",
    "package com.example.myapplication.data.model.request": "package com.example.myapplication.data.remote.dto.request",
    "package com.example.myapplication.data.model.response": "package com.example.myapplication.data.remote.dto.response",
    
    # Imports
    "import com.example.myapplication.data.respository": "import com.example.myapplication.data.repository",
    "import com.example.myapplication.data.model.request": "import com.example.myapplication.data.remote.dto.request",
    "import com.example.myapplication.data.model.response": "import com.example.myapplication.data.remote.dto.response",
    "import com.example.myapplication.data.remote.ApiService": "import com.example.myapplication.data.remote.api.ApiService",
    "import com.example.myapplication.data.remote.RetrofitClient": "import com.example.myapplication.data.remote.network.RetrofitClient",
    "import com.example.myapplication.data.remote.AuthInterceptor": "import com.example.myapplication.data.remote.interceptor.AuthInterceptor",
    "import com.example.myapplication.utils.SingleLiveEvent": "import com.example.myapplication.utils.common.SingleLiveEvent",
    "import com.example.myapplication.utils.Resource": "import com.example.myapplication.utils.resource.Resource",
    
    # Class names
    "AuthRespository": "AuthRepository",
    "BaseRespository": "BaseRepository",
    "MapRespository": "MapRepository",
    "ProfileRespository": "ProfileRepository"
}

# Recursively update files
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            
            new_content = content
            for old, new in replacements.items():
                new_content = new_content.replace(old, new)
                
            # Special package handling for moved remote and utils files
            if file == "ApiService.kt":
                new_content = new_content.replace("package com.example.myapplication.data.remote", "package com.example.myapplication.data.remote.api")
            elif file == "RetrofitClient.kt":
                new_content = new_content.replace("package com.example.myapplication.data.remote", "package com.example.myapplication.data.remote.network")
            elif file == "AuthInterceptor.kt":
                new_content = new_content.replace("package com.example.myapplication.data.remote", "package com.example.myapplication.data.remote.interceptor")
            elif file == "SingleLiveEvent.kt":
                new_content = new_content.replace("package com.example.myapplication.utils", "package com.example.myapplication.utils.common")
            elif file == "Resource.kt":
                new_content = new_content.replace("package com.example.myapplication.utils", "package com.example.myapplication.utils.resource")
            
            if new_content != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Updated {filepath}")
