import grpc, base64, pickle, sys, signal
from bkt_pomdp import choose_best_question
from grpc import StatusCode
from concurrent import futures
from pyBKT.models import Roster
from pyBKT.models.Roster import SkillRoster
from proto import bkt_service_pb2, bkt_service_pb2_grpc

class BktServiceServicer(bkt_service_pb2_grpc.BktServiceServicer):

    def _deserialize_roster(self, raw_b64: str, context):
        try:
            return pickle.loads(base64.b64decode(raw_b64))
        except Exception as e:
            context.abort(StatusCode.INVALID_ARGUMENT, str(e))

    def _serialize_roster(self, roster):
        return base64.b64encode(pickle.dumps(roster)).decode("utf-8")

    def _ensure_skill_and_student(self, roster: Roster, skill: str, student: str):
        """Гарантирует, что у roster есть нужный skill и student."""
        # если навык впервые встречается – заводим пустой SkillRoster,
        # используя тот же pyBKT Model и настройки, что в исходном roster
        if skill not in roster.skill_rosters:
            roster.skill_rosters[skill] = SkillRoster(
                students=list(roster.students),
                skill=skill,
                mastery_state=roster.mastery_state,
                track_progress=roster.track_progress,
                model=roster.model,
            )
        # если студент новый – добавляем
        if student not in roster.skill_rosters[skill].students:
            roster.skill_rosters[skill].add_student(student)
        return roster

    def UpdateRoster(self, request, context):
        # ---------- 1. Десериализация ----------
        roster = self._deserialize_roster(request.roster, context)

        try:
            # ---------- 2. Обновление ----------
            skills_list = request.skills
            for skill in skills_list:
                roster = self._ensure_skill_and_student(roster, skill, request.student)
                # апдейт состояния
                roster.update_state(skill, request.student, int(request.correct == True))
        except Exception as e:
            context.abort(StatusCode.INTERNAL, f"Python error: {e}")

        # ---------- 3. Сериализация результата ----------
        return bkt_service_pb2.UpdateRosterResponse(roster = self._serialize_roster(roster))

    def GetSkillStates(self, request, context):
        # ---------- 1. Десериализация ----------
        roster = self._deserialize_roster(request.roster, context)
        skill_states = []

        try:
            # ---------- 2. Получаем состояния ----------
            skills_list = request.skills
            for skill in skills_list:
                roster = self._ensure_skill_and_student(roster, skill, request.student)
                # получение состояния
                state = roster.get_state(skill, request.student)
                skill_states.append(bkt_service_pb2.SkillState(
                    skill = skill,
                    state = state.state_type.value,
                    correctPrediction = state.get_correct_prob(),
                    statePrediction = state.get_mastery_prob(),
                ))
        except Exception as e:
            context.abort(StatusCode.INTERNAL, f"Python error: {e}")

        # ---------- 3. Сериализация результата ----------
        return bkt_service_pb2.GetSkillStatesResponse(skillStates = skill_states)

    def ChooseBestQuestion(self, request, context):
        src = request.WhichOneof("questionsSource")

        # ---------- 1. Десериализация ----------
        roster = self._deserialize_roster(request.roster, context)

        try:
            # ---------- 2. Выбираем лучший вопрос ----------
            skills_list = request.allSkills
            for skill in skills_list:
                roster = self._ensure_skill_and_student(roster, skill, request.student)

            if src == "candidateQuestions":
                candidate_questions = [
                    tuple(q.targetSkills)
                    for q in request.candidateQuestions.questions
                ]
                max_question_skills_count = None
            else:
                candidate_questions = None
                max_question_skills_count = request.maxQuestionSkillsCount or 3

            best_question = choose_best_question(
                roster,
                skills_list, 
                request.student,
                max_question_skills_count,
                candidate_questions,
            )
        except ValueError as e:
            context.abort(StatusCode.INVALID_ARGUMENT, str(e))
        except Exception as e:
            context.abort(StatusCode.INTERNAL, f"Python error: {e}")

        # ---------- 3. Сериализация результата ----------
        return bkt_service_pb2.ChooseBestQuestionResponse(bestTargetSkills = best_question)

def build_server(port: int = 50051) -> grpc.Server:
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=8))
    bkt_service_pb2_grpc.add_BktServiceServicer_to_server(BktServiceServicer(), server)
    server.add_insecure_port(f"[::]:{port}")
    return server

def graceful_shutdown(server: grpc.Server, *args):
    print("[PY] shutting down …", flush=True)
    server.stop(0)
    sys.exit(0)

if __name__ == "__main__":
    port = 50051
    grpc_server = build_server(port = port)
    signal.signal(signal.SIGTERM, lambda *a: graceful_shutdown(grpc_server))
    signal.signal(signal.SIGINT,  lambda *a: graceful_shutdown(grpc_server))
    
    grpc_server.start()
    print(f"[PY] BKT-service listening on :{port}", flush=True)
    grpc_server.wait_for_termination()
