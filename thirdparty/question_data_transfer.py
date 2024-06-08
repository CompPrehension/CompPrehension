import os
import json
import logging
from sqlalchemy import create_engine, Column, Integer, String, ForeignKey, JSON
from sqlalchemy.orm import sessionmaker, relationship, declarative_base

# Database connection setup
DATABASE_URL = "mysql+mysqlconnector://root:root@localhost/test_db"
engine = create_engine(DATABASE_URL)
Session = sessionmaker(bind=engine)
session = Session()

Base = declarative_base()


# Define the QuestionMetadataEntity and QuestionDataEntity models
class QuestionMetadataEntity(Base):
    __tablename__ = 'questions_meta'

    id = Column(Integer, primary_key=True, autoincrement=True, nullable=False)
    name = Column(String)
    domain_shortname = Column(String)
    template_id = Column(String)
    q_data_graph = Column(String)
    tag_bits = Column(Integer)
    concept_bits = Column(Integer)
    law_bits = Column(Integer)
    violation_bits = Column(Integer)
    trace_concept_bits = Column(Integer)
    solution_structural_complexity = Column(Integer)
    integral_complexity = Column(Integer)
    solution_steps = Column(Integer)
    distinct_errors_count = Column(Integer)
    _version = Column(Integer)
    used_count = Column(Integer)
    date_last_used = Column(String)
    last_attempt_id = Column(Integer)
    structure_hash = Column(String)
    origin = Column(String)
    qrlog_ids = Column(JSON)
    date_created = Column(String)
    question_data_id = Column(Integer, ForeignKey('questions_data.id'))

    question_data = relationship("QuestionDataEntity", back_populates="question_metadata")


class QuestionDataEntity(Base):
    __tablename__ = 'questions_data'

    id = Column(Integer, primary_key=True, autoincrement=False, nullable=False)
    data = Column(JSON)

    question_metadata = relationship("QuestionMetadataEntity", back_populates="question_data")


# Base directory for JSON files
BASE_DIR = "C:\\Temp2\\compp"

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')


def process_metadata_entities():
    metadata_entities = session.query(QuestionMetadataEntity).all()

    for entity in metadata_entities:
        # Check if the question_data_id is already set
        if entity.question_data_id is not None:
            logging.info(f"Skipping entity with id {entity.id} and path {entity.q_data_graph} as it already has question_data_id set")
            continue

        json_file_path = os.path.join(BASE_DIR, get_middle_path(str(entity.domain_shortname)), str(entity.q_data_graph))
        # Normalize the path to use correct slashes for the OS
        json_file_path = os.path.normpath(json_file_path)
        logging.info(f"Processing file: {json_file_path}")

        if os.path.exists(json_file_path):
            try:
                with open(json_file_path, 'r', encoding='utf-8') as file:
                    json_data = json.load(file)

                # Create a new QuestionDataEntity with the JSON data and the same id as the QuestionMetadataEntity
                question_data = QuestionDataEntity(id=entity.id, data=json_data)
                session.add(question_data)
                session.flush()  # Flush to ensure the data is added to the session

                # Update the QuestionMetadataEntity with the new question_data_id
                entity.question_data_id = question_data.id
                logging.info(f"Successfully processed and updated: {json_file_path}")
            except Exception as e:
                logging.error(f"Error processing file {json_file_path}: {e}")
        else:
            logging.warning(f"File does not exist: {json_file_path}")

    session.commit()


def get_middle_path(domain_short_name: str) -> str:
    if domain_short_name == "expression":
        return "expression"
    elif domain_short_name == "ctrl_flow":
        return "control_flow"
    else:
        return domain_short_name


# Execute the process
process_metadata_entities()

# Close the session
session.close()
