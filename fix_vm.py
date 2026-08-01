import os

base_dir = r"e:\HIT_Product\app\src\main\java\com\example\myapplication\ui\home\chat"
files_to_fix = [
    r"conversation\ConversationViewModel.kt",
    r"chatroom\ChatViewModel.kt",
    r"group\GroupViewModel.kt"
]

for rel in files_to_fix:
    path = os.path.join(base_dir, rel)
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Need to add import and fix class declaration
    if "import android.app.Application" not in content:
        content = content.replace("import com.example.myapplication.ui.base.BaseViewModel",
                                  "import android.app.Application\nimport com.example.myapplication.ui.base.BaseViewModel")
    
    import re
    content = re.sub(r'class (\w+) : BaseViewModel\(\)', r'class \1(application: Application) : BaseViewModel<Any>(application)', content)
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Fixed {path}")
