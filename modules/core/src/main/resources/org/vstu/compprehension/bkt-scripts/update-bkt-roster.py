"""
Обновление pyBKT-Roster одним ответом студента.

Usage:
  python3 update-bkt-roster.py \
      --roster  "<base64_pickle_string>" \
      --student 123 \
      --correct 1 \
      --skills  '["Some skill","Some other skill"]'
"""
import argparse, base64, pickle, json, sys
from pyBKT.models import Roster
from pyBKT.models.Roster import SkillRoster

def parse_skills(raw: str):
    """JSON-массив или comma-separated строка → List[str]."""
    try:
        obj = json.loads(raw)
        if isinstance(obj, list):
            return [str(s) for s in obj]
    except json.JSONDecodeError:
        pass
    return [s.strip() for s in raw.split(",") if s.strip()]

def main():
    p = argparse.ArgumentParser(description="Update pyBKT roster with a single student response")
    p.add_argument("--roster",  required=True, help="Base64-encoded pickle of pyBKT.models.Roster")
    p.add_argument("--student", required=True, help="Student identifier (string or int)")
    p.add_argument("--correct", required=True, type=int, choices=[0,1],
                   help="1 – correct answer, 0 – incorrect")
    p.add_argument("--skills",  required=True,
                   help="JSON array or comma-separated list of skill names")
    args = p.parse_args()

    # ---------- 1. Десериализация ----------
    try:
        roster: Roster = pickle.loads(base64.b64decode(args.roster))
    except Exception as e:
        sys.exit(f"Cannot load roster: {e}")

    # ---------- 2. Обновление ----------
    skills_list = parse_skills(args.skills)
    for skill in skills_list:
        # 2.1. если навык впервые встречается – заводим пустой SkillRoster,
        #      используя тот же pyBKT Model и настройки, что в исходном roster
        if skill not in roster.skill_rosters:
            roster.skill_rosters[skill] = SkillRoster(
                students=list(roster.students),
                skill=skill,
                mastery_state=roster.mastery_state,
                track_progress=roster.track_progress,
                model=roster.model,
            )
        # 2.2. если студент новый – добавляем
        if args.student not in roster.skill_rosters[skill].students:
            roster.skill_rosters[skill].add_student(args.student)
        # 2.3. апдейт состояния
        roster.update_state(skill, args.student, args.correct)

    # ---------- 3. Сериализация результата ----------
    print(base64.b64encode(pickle.dumps(roster)).decode("utf-8"))

if __name__ == "__main__":
    main()
